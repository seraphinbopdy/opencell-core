package org.meveo.admin.job.utils;

import org.meveo.model.dunning.DunningPolicy;
import org.meveo.model.dunning.DunningPolicyRule;
import org.meveo.model.dunning.DunningPolicyRuleLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DunningUtils {

    private static final Logger log = LoggerFactory.getLogger(DunningUtils.class);

    /**
     * Get rules
     * @param policy Dunning policy
     * @return Rules
     */
    public static String getRules(DunningPolicy policy) {
        StringBuilder rulesBuilder = new StringBuilder();

        for (DunningPolicyRule rule : policy.getDunningPolicyRules()) {
            if (rulesBuilder.length() > 0) {
                rulesBuilder.append(" ").append(rule.getRuleJoint()).append(" ");
            }
            rulesBuilder.append("(");
            boolean firstRuleLine = true;
            for (DunningPolicyRuleLine ruleLine : rule.getDunningPolicyRuleLines()) {
                if (!firstRuleLine) {
                    rulesBuilder.append(" ").append(ruleLine.getRuleLineJoint()).append(" ");
                }
                rulesBuilder.append(ruleLine.getPolicyConditionTarget())
                        .append(" ")
                        .append(getOperator(ruleLine.getPolicyConditionOperator()))
                        .append(" '")
                        .append(ruleLine.getPolicyConditionTargetValue())
                        .append("'");
                firstRuleLine = false;
            }
            rulesBuilder.append(")");
        }

        String rules = rulesBuilder.toString().replace("))", ")")
                .replace("'true'", "true").replace("'false'", "false")
                .replace("'TRUE'", "true").replace("'FALSE'", "false")
                .replace("AND", "&&").replace("OR", "||");

        log.info("Policy rules: {}", rules);
        return rules;
    }



    /**
     * Get operator
     * @param operator Operator
     * @return Operator
     */
    private static String getOperator(String operator) {
        switch (operator) {
            case "EQUALS":
                return "==";
            case "NOT_EQUALS":
                return "!=";
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }
}
