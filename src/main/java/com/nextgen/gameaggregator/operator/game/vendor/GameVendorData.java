package com.nextgen.gameaggregator.operator.game.vendor;

import com.nextgen.gameaggregator.entity.custom.IGameVendor;
import lombok.Data;

import java.util.List;

@Data
public class GameVendorData {

    private List<IGameVendor> vendors;
}
