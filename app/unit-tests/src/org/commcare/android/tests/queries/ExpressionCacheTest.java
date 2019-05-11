package org.commcare.android.tests.queries;

import org.commcare.CommCareApplication;
import org.commcare.android.CommCareTestRunner;
import org.commcare.android.util.TestUtils;
import org.commcare.cases.instance.CaseChildElement;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.model.trace.EvaluationTraceReporter;
import org.javarosa.core.model.trace.ReducingTraceReporter;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

/**
 * Created by amstone326 on 2/2/18.
 */

@Config(application = CommCareApplication.class)
@RunWith(CommCareTestRunner.class)
public class ExpressionCacheTest {

    private FormInstance mainFormInstance;
    private TreeElement formRoot;

    @Before
    public void setup() {
        formRoot = new TreeElement("data");
        mainFormInstance = new FormInstance(formRoot);

        TestUtils.initializeStaticTestStorage();
        TestUtils.processResourceTransaction("/inputs/case_test_model_query_lookups.xml");
    }

    @Test
    public void testNumExpressionsCached() throws XPathSyntaxException {
        String expression = "join(',',instance('casedb')/casedb/case[@case_type='unit_test_child_child']" +
                "[@status='open'][true() and instance('casedb')/casedb/case[@case_id = " +
                "instance('casedb')/casedb/case[@case_id=current()/index/parent]" +
                "/index/parent]/test = 'true']/@case_id)";
        Assert.assertEquals(2, getNumExpressionsCached(expression, true));
        Assert.assertEquals(0, getNumExpressionsCached(expression, false));

        expression = "if(true(), now() + 1, now())";
        Assert.assertEquals(2, getNumExpressionsCached(expression, true));

        formRoot.addChild(new TreeElement("next_refill_due_date"));
        expression = "date(/data/next_refill_due_date) <= today()";
        Assert.assertEquals(0, getNumExpressionsCached(expression, true));

        expression = "random()";
        Assert.assertEquals(0, getNumExpressionsCached(expression, true));
    }

    private long getNumExpressionsCached(String exprString, boolean enableCaching) throws XPathSyntaxException {
        EvaluationContext ec = TestUtils.getEvaluationContextWithoutSession(mainFormInstance);
        EvaluationTraceReporter reporter = new ReducingTraceReporter(true);
        ec.setDebugModeOn(reporter);
        if (enableCaching) {
            ec.enableExpressionCaching();
        }
        for (int i = 0; i < 2; i++) {
            evaluate(exprString, ec);
        }

        return reporter.getCollectedTraces().stream()
                .filter(trace -> trace.evaluationUsedExpressionCache()).count();
    }

    private static void evaluate(String xpath, EvaluationContext ec) throws XPathSyntaxException {
        XPathExpression expr = XPathParseTool.parseXPath(xpath);
        expr.eval(ec);
    }

    @Test
    public void testAccuracyWithCaching() {
        EvaluationContext ec = TestUtils.getEvaluationContextWithoutSession(mainFormInstance);
        ec.enableExpressionCaching();
        for (int i = 0; i < 3; i++) {
            CaseDbQueryTest.evaluate("join(',',instance('casedb')/casedb/case[@case_type='unit_test_child_child']" +
                            "[@status='open'][true() and instance('casedb')/casedb/case[@case_id = " +
                            "instance('casedb')/casedb/case[@case_id=current()/index/parent]/index/parent]/test = 'true']/@case_id)",
                    "child_ptwo_one_one,child_one_one",
                    ec);
        }
    }

    @Test
    public void testCheckForContextNodeCacheability() throws XPathSyntaxException {
        EvaluationContext rootContextEC = TestUtils.getEvaluationContextWithoutSession(mainFormInstance);

        TreeElement questionElt = new TreeElement("q1");
        formRoot.addChild(questionElt);
        EvaluationContext questionContextEC = new EvaluationContext(rootContextEC,
                TreeReference.buildRefFromTreeElement(questionElt));

        testContextNodeCacheability(". = 'yes'", questionContextEC, false);
        testContextNodeCacheability("@id ='1' and true()", questionContextEC, false);
        testContextNodeCacheability("current()/@id", questionContextEC, false);
        testContextNodeCacheability("max(., ../q2)", questionContextEC, false);
        testContextNodeCacheability("true()", questionContextEC, true);
        testContextNodeCacheability(
                "instance('casedb')/casedb/case[@case_id=current()/index/parent]",
                questionContextEC, true);

        CaseChildElement caseDbChildElement =
                (CaseChildElement)TestUtils.getCaseDbInstance().getRoot().getChildrenWithName("case").get(0);
        EvaluationContext caseEltContextEC = questionContextEC.rescope(
                TreeReference.buildRefFromTreeElement(caseDbChildElement), 1,
                questionContextEC.getCurrentQueryContext());

        testContextNodeCacheability("current()/@id",
                caseEltContextEC, false);
        testContextNodeCacheability("max(@id, current()/index/parent/@id)",
                caseEltContextEC, false);
        testContextNodeCacheability("@id ='1' and true()",
                caseEltContextEC, true);
        testContextNodeCacheability(
                "instance('casedb')/casedb/case[@case_id=current()/index/parent]",
                caseEltContextEC, true);
    }

    private void testContextNodeCacheability(String expressionString, EvaluationContext ec, boolean expected)
            throws XPathSyntaxException {
        XPathExpression xpe = XPathParseTool.parseXPath(expressionString);
        Assert.assertEquals(expected, xpe.relevantContextNodesAreCacheable(ec));
    }

}
