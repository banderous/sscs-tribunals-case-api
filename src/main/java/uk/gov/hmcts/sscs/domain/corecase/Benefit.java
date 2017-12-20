package uk.gov.hmcts.sscs.domain.corecase;

public enum Benefit {

    UNIVERSAL_CREDIT("Universal Credit", "Universal Credit", "001"),
    PIP("PIP", "Personal Independence Payment (PIP)", "002"),
    INCOME_SUPPORT("Income Support", "Income Support", "077"),
    CARERS_ALLOWANCE("Carers Allowance", "Carers Allowance", "070"),
    JOB_SEEKERS_ALLOWANCE("Job Seekers Allowance", "Job Seekers Allowance", "073");

    private final String type;
    private final String fullDesciption;
    private final String code;

    Benefit(String type, String fullDesciption, String code) {
        this.type = type;
        this.fullDesciption = fullDesciption;
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public String getFullDesciption() {
        return fullDesciption;
    }

    public String getCode() {
        return code;
    }

    public static Benefit getBenefitByType(String x) {
        Benefit b = null;
        for (Benefit type : Benefit.values()) {
            if (type.getType().equals(x)) {
                b = type;
            }
        }
        return b;
    }

    //Used for SYA
    public static Benefit getBenefitByFullDescription(String x) {
        Benefit b = null;
        for (Benefit type : Benefit.values()) {
            if (type.getFullDesciption().equals(x)) {
                b = type;
            }
        }
        return b;
    }
}
