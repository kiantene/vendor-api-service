package com.nextgen.gameaggregator.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nextgen.gameaggregator.entity.AgentVendorLine;
import com.nextgen.gameaggregator.entity.VendorLine;
import com.nextgen.gameaggregator.repository.AgentVendorLineRepository;
import com.nextgen.gameaggregator.repository.VendorLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "vendorLine/")
public class VendorLineController {

    @Autowired
    VendorLineRepository vendorLineRepository;

    @Autowired
    ControllerServices controllerServices;

    @Autowired
    AgentVendorLineRepository agentVendorLineRepository;

    //for QA to test disable vendor line status
    @PostMapping(path = "/status")
    public ResponseEntity<Map<String, String>> status(@RequestBody ObjectNode json) {
        HashMap<String, String> responseMap = new HashMap<>();

        VendorLine vendorLine = vendorLineRepository.findById(Integer.parseInt(json.get("id").toString())).orElse(null);
        controllerServices.clearVendorLines();

        List<AgentVendorLine> agentVendorLines = agentVendorLineRepository.findByVendorLineId(Integer.parseInt(json.get("id").toString()));

        if (vendorLine == null) {
            responseMap.put("status", "fail, vendor line not found");
            responseMap.put("id", json.get("id").toString());

            return new ResponseEntity<>(
                    responseMap,
                    HttpStatus.BAD_REQUEST);
        }

        vendorLine.setStatus(Integer.parseInt(json.get("status").toString()));
        vendorLineRepository.save(vendorLine);


        if (!agentVendorLines.isEmpty()) {
            for (AgentVendorLine agentVendorLine : agentVendorLines) {
                agentVendorLine.setStatus(Integer.parseInt(json.get("status").toString()));
                agentVendorLineRepository.save(agentVendorLine);
            }
        }

        responseMap.put("status", "Success");
        responseMap.put("id", json.get("id").toString());

        return new ResponseEntity<>(
                responseMap,
                HttpStatus.OK);


    }

    @PostMapping(path = "/agentVendorLineStatus")
    public ResponseEntity<Map<String, String>> agentVendorLineStatus(@RequestBody ObjectNode json) {
        HashMap<String, String> responseMap = new HashMap<>();

        List<AgentVendorLine> agentVendorLines = agentVendorLineRepository.findByAgentIdAndVendorLineId(
                Integer.parseInt(json.get("agentId").toString()),Integer.parseInt(json.get("vendorLineId").toString()));

        if (agentVendorLines.isEmpty()) {
            responseMap.put("status", "fail, Agent vendor line not found");
            responseMap.put("agentId", json.get("agentId").toString());
            responseMap.put("vendorLineId", json.get("vendorLineId").toString());

            return new ResponseEntity<>(
                    responseMap,
                    HttpStatus.BAD_REQUEST);
        }

        for (AgentVendorLine agentVendorLine : agentVendorLines) {
            agentVendorLine.setStatus(Integer.parseInt(json.get("status").toString()));
            agentVendorLineRepository.save(agentVendorLine);
        }

        responseMap.put("status", "Success");
        responseMap.put("agentId", json.get("agentId").toString());
        responseMap.put("vendorLineId", json.get("vendorLineId").toString());

        return new ResponseEntity<>(
                responseMap,
                HttpStatus.OK);

    }
}
