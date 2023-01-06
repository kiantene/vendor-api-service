package com.nextgen.gameaggregator.data.mariadb.reader.manager;


import com.nextgen.gameaggregator.data.mariadb.reader.entity.AgentPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentPlayerManager extends JpaRepository<AgentPlayer, Long> {
}
