package org.commcare.models;

import android.util.Log;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import org.commcare.AppUtils;
import org.commcare.CommCareApplication;
import org.commcare.android.logging.ReportingUtils;
import org.commcare.cases.entity.Entity;
import org.commcare.cases.entity.NodeEntityFactory;
import org.commcare.models.database.DbUtil;
import org.commcare.models.database.SqlStorage;
import org.commcare.models.database.user.models.EntityStorageCache;
import org.commcare.modern.database.TableBuilder;
import org.commcare.suite.model.Detail;
import org.commcare.suite.model.DetailField;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.utils.CacheHost;
import org.javarosa.core.util.OrderedHashtable;
import org.javarosa.xpath.expr.XPathExpression;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/**
 * @author ctsims
 */
public class AsyncNodeEntityFactory extends NodeEntityFactory {
    private static final String TAG = AsyncNodeEntityFactory.class.getSimpleName();
    private final OrderedHashtable<String, XPathExpression> mVariableDeclarations;

    private final Hashtable<String, AsyncEntity> mEntitySet = new Hashtable<>();
    private final EntityStorageCache mEntityCache;

    private CacheHost mCacheHost = null;
    private Boolean mTemplateIsCachable = null;
    private static final Object mAsyncLock = new Object();
    private Thread mAsyncPrimingThread;

    // Don't show entity list until we primeCache and caches all fields
    private final boolean isBlockingAsyncMode;

    public AsyncNodeEntityFactory(Detail d, EvaluationContext ec) {
        super(d, ec);

        mVariableDeclarations = detail.getVariableDeclarations();
        mEntityCache = new EntityStorageCache("case");
        isBlockingAsyncMode = detail.hasSortField();
    }

    @Override
    public Entity<TreeReference> getEntity(TreeReference data) {
        EvaluationContext nodeContext = new EvaluationContext(ec, data);

        mCacheHost = nodeContext.getCacheHost(data);

        String mCacheIndex = null;
        if (mTemplateIsCachable == null) {
            mTemplateIsCachable = mCacheHost != null && mCacheHost.isReferencePatternCachable(data);
        }
        if (mTemplateIsCachable) {
            if (mCacheHost == null) {
                Log.d(TAG, "Template is cachable, but there's no cache host for this instance?");
            } else {
                mCacheIndex = mCacheHost.getCacheIndex(data);
            }
        }

        String entityKey = loadCalloutDataMapKey(nodeContext);
        AsyncEntity entity =
                new AsyncEntity(detail.getFields(), nodeContext, data, mVariableDeclarations,
                        mEntityCache, mCacheIndex, detail.getId(), entityKey);

        if (mCacheIndex != null) {
            mEntitySet.put(mCacheIndex, entity);
        }
        return entity;
    }

    @Override
    protected void setEvaluationContextDefaultQuerySet(EvaluationContext ec,
                                                       List<TreeReference> result) {

        //Don't do anything for asynchronous lists. In theory the query set could help expand the
        //first cache more quickly, but otherwise it's just keeping around tons of cases in memory
        //that don't even need to be loaded.
    }


    /**
     * Bulk loads search field cache from db.
     * Note that the cache is lazily built upon first case list search.
     */
    private void primeCache() {
        if (mTemplateIsCachable == null || !mTemplateIsCachable || mCacheHost == null) {
            return;
        }

        String[][] cachePrimeKeys = mCacheHost.getCachePrimeGuess();
        if (cachePrimeKeys == null) {
            return;
        }

        Vector<Integer> sortKeys = new Vector<>();
        String validKeys = buildValidKeys(sortKeys, detail.getFields());
        if ("".equals(validKeys)) {
            return;
        }

        //Create our full args tree. We need the elements from the cache primer
        //along with the specific keys we wanna pull out

        String[] args = new String[cachePrimeKeys[1].length + sortKeys.size()];
        System.arraycopy(cachePrimeKeys[1], 0, args, 0, cachePrimeKeys[1].length);

        for (int i = 0; i < sortKeys.size(); ++i) {
            args[cachePrimeKeys[1].length + i] = getCacheKey(detail.getId(), String.valueOf(sortKeys.get(i)));
        }

        String[] names = cachePrimeKeys[0];
        String whereClause = buildKeyNameWhereClause(names);

        long now = System.currentTimeMillis();

        SQLiteDatabase db = CommCareApplication.instance().getUserDbHandle();

        String sqlStatement = "SELECT entity_key, cache_key, value FROM entity_cache JOIN AndroidCase ON entity_cache.entity_key = AndroidCase.commcare_sql_id WHERE " +
                whereClause + " AND " + EntityStorageCache.COL_APP_ID + " = '" + AppUtils.getCurrentAppId() +
                "' AND cache_key IN " + validKeys;
        if (SqlStorage.STORAGE_OUTPUT_DEBUG) {
            DbUtil.explainSql(db, sqlStatement, args);
        }

        populateEntitySet(db, sqlStatement, args);

        if (SqlStorage.STORAGE_OUTPUT_DEBUG) {
            Log.d(TAG, "Sequential Cache Load: " + (System.currentTimeMillis() - now) + "ms");
        }
    }

    private String buildValidKeys(Vector<Integer> sortKeys, DetailField[] fields) {
        String validKeys = "(";
        boolean added = false;
        for (int i = 0; i < fields.length; ++i) {
            //We're only gonna pull out the fields we can index/sort on
            if (fields[i].getSort() != null) {
                sortKeys.add(i);
                validKeys += "?, ";
                added = true;
            }
        }
        if (added) {
            return validKeys.substring(0, validKeys.length() - 2) + ")";
        } else {
            return "";
        }
    }

    public static String getCacheKey(String detailId, String mFieldId) {
        return detailId + "_" + mFieldId;
    }

    private String buildKeyNameWhereClause(String[] names) {
        String whereClause = "";
        for (int i = 0; i < names.length; ++i) {
            whereClause += TableBuilder.scrubName(names[i]) + " = ?";
            if (i + 1 < names.length) {
                whereClause += " AND ";
            }
        }
        return whereClause;
    }

    private void populateEntitySet(SQLiteDatabase db, String sqlStatement, String[] args) {
        //TODO: This will _only_ query up to about a meg of data, which is an un-great limitation.
        //Should probably split this up SQL LIMIT based looped
        //For reference the current limitation is about 10k rows with 1 field each.
        Cursor walker = db.rawQuery(sqlStatement, args);
        while (walker.moveToNext()) {
            String entityId = walker.getString(walker.getColumnIndex("entity_key"));
            String cacheId = walker.getString(walker.getColumnIndex("cache_key"));
            String val = walker.getString(walker.getColumnIndex("value"));
            if (this.mEntitySet.containsKey(entityId)) {
                this.mEntitySet.get(entityId).setSortData(cacheId, val);
            }
        }
        walker.close();
    }

    @Override
    protected void prepareEntitiesInternal(List<Entity<TreeReference>> entities) {
        // if blocking mode load cache on the same thread and set any data thats not cached
        if (isBlockingAsyncMode) {
            primeCache();
            setUnCachedData(entities);
        } else {
            // otherwise we want to show the entity list asap and hence want to offload the loading cache part to a separate
            // thread while caching any uncached data later on UI thread during Adapter's getView
            synchronized (mAsyncLock) {
                if (mAsyncPrimingThread == null) {
                    mAsyncPrimingThread = new Thread(this::primeCache);
                    mAsyncPrimingThread.start();
                }
            }
        }
    }

    private void setUnCachedData(List<Entity<TreeReference>> entities) {
        for (int i = 0; i < entities.size(); i++) {
            AsyncEntity e = (AsyncEntity)entities.get(i);
            for (int col = 0; col < e.getNumFields(); ++col) {
                e.getSortField(col);
            }
        }
    }

    @Override
    protected boolean isEntitySetReadyInternal() {
        synchronized (mAsyncLock) {
            return mAsyncPrimingThread == null || !mAsyncPrimingThread.isAlive();
        }
    }

    public boolean isBlockingAsyncMode() {
        return isBlockingAsyncMode;
    }
}
