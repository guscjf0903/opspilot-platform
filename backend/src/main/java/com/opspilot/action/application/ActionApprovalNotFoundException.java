package com.opspilot.action.application;

import java.util.UUID;

public class ActionApprovalNotFoundException extends RuntimeException {

    public ActionApprovalNotFoundException(UUID approvalId) {
        super("Action approval request not found: " + approvalId);
    }
}
