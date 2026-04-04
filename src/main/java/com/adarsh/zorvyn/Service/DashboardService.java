package com.adarsh.zorvyn.Service;

import com.adarsh.zorvyn.Entity.Record;
import com.adarsh.zorvyn.Entity.Type;
import com.adarsh.zorvyn.Repository.RecordsRepository;
import com.adarsh.zorvyn.Response.FinanceDashboardResponse;
import com.adarsh.zorvyn.Response.RecordResponse;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final RecordsRepository recordsRepository;

    public DashboardService(RecordsRepository recordsRepository) {
        this.recordsRepository = recordsRepository;
    }

    public FinanceDashboardResponse getDashboard(){

        List<Record> records = recordsRepository.findAll();

        double totalIncome = records.stream()
                .filter(r -> r.getType() == Type.INCOME)
                .mapToDouble(Record::getAmount)
                .sum();

        double totalExpense = records.stream()
                .filter(r -> r.getType() == Type.EXPENSE)
                .mapToDouble(Record::getAmount)
                .sum();

        double netBalance = totalIncome - totalExpense;

        FinanceDashboardResponse response = new FinanceDashboardResponse();
        response.setTotalIncome(totalIncome);
        response.setTotalExpense(totalExpense);
        response.setNetBalance(netBalance);
        response.setCategoryTotals(getCategorySummary());
        response.setMonthlyTrends(getMonthlySummary());
        response.setRecentTransactions(getRecentTransactions());

        return response;
    }

    public Map<String, Double> getCategorySummary(){

        return recordsRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        Record::getCategory,
                        Collectors.summingDouble(r ->
                                r.getType() == Type.INCOME ? r.getAmount() : -r.getAmount()
                        )
                ));
    }

    public Map<String, Double> getMonthlySummary(){

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");

        return recordsRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        r -> format.format(r.getDate()),
                        TreeMap::new,
                        Collectors.summingDouble(r ->
                                r.getType() == Type.INCOME ? r.getAmount() : -r.getAmount()
                        )
                ));
    }

    public List<RecordResponse> getRecentTransactions(){

        return recordsRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .limit(5)
                .map(this::mapToResponse)
                .toList();
    }


    private RecordResponse mapToResponse(Record record){
        return RecordResponse.builder()
                .id(record.getId())
                .amount(record.getAmount())
                .type(record.getType())
                .category(record.getCategory())
                .date(record.getDate())
                .note(record.getNote())
                .build();
    }
}