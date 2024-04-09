package com.nextgen.gameaggregator.operator.game.hottoplist;

import java.util.List;

import lombok.Data;

@Data
public class HotTopGameListDto {
	
	private String name;
	private String currencyCode;
	private String code;
	private String gameCode;
	private String gameName;
	private String categroryCode;
	private String imageSquare;
	private String imageLandscape;
	private List<String> languageCode;
	private List<String> platformCode;

}
