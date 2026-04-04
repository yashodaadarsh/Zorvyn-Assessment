package com.adarsh.zorvyn.Service;

import com.adarsh.zorvyn.Entity.Type;
import com.adarsh.zorvyn.Entity.User;
import com.adarsh.zorvyn.Entity.Record;
import com.adarsh.zorvyn.Repository.RecordsRepository;
import com.adarsh.zorvyn.Request.RecordRequest;
import com.adarsh.zorvyn.Response.RecordResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class RecordsService {

    @Autowired
    private RecordsRepository recordsRepository;

    public RecordResponse createRecord(RecordRequest recordRequest, User user) {
        Record record = convertToRecord(recordRequest);
        record.setCreatedBy(user);

        Record savedRecord = recordsRepository.save(record);
        return convertToRecordResponse(savedRecord);
    }

    public Page<RecordResponse> getFilteredRecords(Type type, String category, Date from, Date to, String search, Pageable pageable) {
        return recordsRepository.findWithFilters(type, category, from, to, search, pageable)
                .map(this::convertToRecordResponse);
    }

    public RecordResponse updateRecord(int id, RecordRequest request, User user) {

        Record record = recordsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));


        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setNote(request.getNote());

        if (request.getDate() != null) {
            record.setDate(request.getDate());
        }

        Record updated = recordsRepository.save(record);

        return convertToRecordResponse(updated);
    }

    public void deleteRecord(int id) {

        Record record = recordsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        recordsRepository.delete(record);
    }

    private Record convertToRecord(RecordRequest recordRequest) {
        return Record.builder()
                .amount(recordRequest.getAmount())
                .type(recordRequest.getType())
                .category(recordRequest.getCategory())
                .date(new Date())
                .note(recordRequest.getNote())
                .build();
    }

    private RecordResponse convertToRecordResponse(Record record) {
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