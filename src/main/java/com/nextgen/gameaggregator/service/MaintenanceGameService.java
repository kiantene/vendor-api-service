package com.nextgen.gameaggregator.service;

import com.nextgen.gameaggregator.entity.ga.MaintenanceGame;
import com.nextgen.gameaggregator.entity.ga.MaintenanceGroup;
import com.nextgen.gameaggregator.enums.Status;
import com.nextgen.gameaggregator.exception.DisabledGameException;
import com.nextgen.gameaggregator.repository.ga.reader.MaintenanceGameRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class MaintenanceGameService {
    private final MaintenanceGameRepository maintenanceGameRepository;

    public MaintenanceGameService(MaintenanceGameRepository maintenanceGameRepository) {
        this.maintenanceGameRepository = maintenanceGameRepository;
    }

    public void checkMaintenanceGameStatus(MaintenanceGame maintenanceGame) throws DisabledGameException {
        if (maintenanceGame != null) {
            long currentTime = Instant.now().getEpochSecond();
            MaintenanceGroup maintenanceGroup = maintenanceGame.getMaintenanceGroup();

            // Check if current time is after startTime and either:
            // - endTime is null (permanent maintenance) OR
            // - current time is before endTime (within maintenance window)
            if (currentTime >= maintenanceGroup.getStartTime() &&
                    (maintenanceGroup.getEndTime() == null || currentTime <= maintenanceGroup.getEndTime())) {
                throw new DisabledGameException("Game is under maintenance");
            }
        }
    }

    @Cacheable(value = "VendorGameMaintenance", key = "#vendorGameId", cacheManager = "cacheManager")
    public MaintenanceGame findByVendorGameIdWithActiveStatus(Integer vendorGameId) {
        // maintenance group status 1 - active, 2 - scheduled
        List<Integer> groupStatusList = Arrays.asList(1, 2);
        return maintenanceGameRepository.findByVendorGameIdAndStatusAndMaintenanceGroup_StatusIn(vendorGameId, Status.ACTIVE.code, groupStatusList);
    }
}
