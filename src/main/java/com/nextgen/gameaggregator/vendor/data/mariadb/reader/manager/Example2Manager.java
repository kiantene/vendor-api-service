package com.nextgen.gameaggregator.vendor.data.mariadb.reader.manager;
import com.nextgen.gameaggregator.vendor.data.mariadb.reader.entity.Example2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Example2Manager extends JpaRepository<Example2, Long> {

}
