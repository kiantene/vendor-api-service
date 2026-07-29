package com.nextgen.gameaggregator.repository.ga.reader;

import com.nextgen.gameaggregator.entity.ga.MaintenanceGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceGameRepository extends JpaRepository<MaintenanceGame, Integer> {
    MaintenanceGame findByVendorGameIdAndStatusAndMaintenanceGroup_StatusIn(Integer vendorGameId, Integer gameStatus, List<Integer> groupStatus);
}
