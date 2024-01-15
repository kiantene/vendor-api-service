package com.nextgen.gameaggregator.repository.ga.writer;

import com.nextgen.gameaggregator.entity.ga.VendorLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorLineRepository extends JpaRepository<VendorLine, Integer> {
    VendorLine findByIdAndStatus(Integer id, Integer status);

}
