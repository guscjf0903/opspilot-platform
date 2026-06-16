package com.opspilot.trends.application.port.out;

import com.opspilot.trends.domain.CostDailySnapshot;
import java.time.LocalDate;
import java.util.List;

public interface CostDailySnapshotStore {

    List<CostDailySnapshot> upsertAll(List<CostDailySnapshot> snapshots);

    List<CostDailySnapshot> find(
            String clusterId,
            String namespace,
            String workloadName,
            LocalDate from,
            LocalDate to
    );
}
