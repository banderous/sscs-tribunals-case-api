package uk.gov.hmcts.sscs.domain.corecase;

public class SmsNotify {

    private Boolean wantsSmsNotifications;

    private String smsNumber;

    private Boolean useSameNumber;

    public Boolean isWantsSmsNotifications() {
        return wantsSmsNotifications;
    }

    public void setWantsSmsNotifications(Boolean wantsSmsNotifications) {
        this.wantsSmsNotifications = wantsSmsNotifications;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public void setSmsNumber(String smsNumber) {
        this.smsNumber = smsNumber;
    }

    public Boolean isUseSameNumber() {
        return useSameNumber;
    }

    public void setUseSameNumber(Boolean useSameNumber) {
        this.useSameNumber = useSameNumber;
    }

    @Override
    public String toString() {
        return "SmsNotify{"
                + " wantsSmsNotifications=" + wantsSmsNotifications
                + ", smsNumber='" + smsNumber + '\''
                + ", useSameNumber=" + useSameNumber
                + '}';
    }
}
