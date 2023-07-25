package com.nextgen.gameaggregator.vendor.playngo.api.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nextgen.gameaggregator.vendor.playngo.vo.CommonVo;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "release")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReleaseVo extends CommonVo {
}
