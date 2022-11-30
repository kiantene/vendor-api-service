package com.nextgen.gameaggregator.vendorapiservice.data.mariadb.writer.manager;
import com.nextgen.gameaggregator.vendorapiservice.data.mariadb.writer.entity.Example1;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Example1Manager extends JpaRepository<Example1, Long> {

}
