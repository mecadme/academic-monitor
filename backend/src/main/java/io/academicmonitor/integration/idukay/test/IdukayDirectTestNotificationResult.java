package io.academicmonitor.integration.idukay.test;

record IdukayDirectTestNotificationResult(boolean sent, IdukayDirectTestNotificationFailure failure) {

    static IdukayDirectTestNotificationResult successful() {
        return new IdukayDirectTestNotificationResult(true, null);
    }

    static IdukayDirectTestNotificationResult failed(IdukayDirectTestNotificationFailure failure) {
        return new IdukayDirectTestNotificationResult(false, failure);
    }
}
