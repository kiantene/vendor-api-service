package com.nextgen.gameaggregator.repository.wallet.reader;

import com.nextgen.gameaggregator.entity.wallet.AccessKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessKeyReaderRepository  extends JpaRepository<AccessKey, Integer> {



    AccessKey findFirstByOrderById();

}
