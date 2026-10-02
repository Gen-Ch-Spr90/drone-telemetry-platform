package com.limloch.telemetry.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestionAuditService {

    private final JdbcTemplate jdbcTemplate;

    public IngestionAuditService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long start(String sourceFile) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO ingestion_audit (source_file, started_at, status)
                VALUES (?, CURRENT_TIMESTAMP, 'PARTIAL') RETURNING id
                """, Long.class, sourceFile);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(long auditId, int pointsIngested) {
        jdbcTemplate.update("""
                UPDATE ingestion_audit SET completed_at = CURRENT_TIMESTAMP,
                    points_ingested = ?, status = 'SUCCESS' WHERE id = ?
                """, pointsIngested, auditId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(long auditId, String message) {
        jdbcTemplate.update("""
                UPDATE ingestion_audit SET completed_at = CURRENT_TIMESTAMP,
                    status = 'FAILED', error_message = ? WHERE id = ?
                """, message, auditId);
    }
}