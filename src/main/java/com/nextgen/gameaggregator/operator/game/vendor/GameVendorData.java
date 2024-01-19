package com.nextgen.gameaggregator.operator.game.vendor;

import com.nextgen.gameaggregator.entity.ga.custom.IGameVendor;
import lombok.Data;

import java.util.List;

@Data
public class GameVendorData {

    private List<IGameVendor> vendors;
}
