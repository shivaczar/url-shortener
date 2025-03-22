package org.bitly.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_logs")
public class RequestLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime timestamp;
    private String method;
    private String url;
    private String userAgent;
    private String ip;

    // Constructors
    public RequestLog() {}

    public RequestLog(LocalDateTime timestamp, String method, String url, String userAgent, String ip) {
        this.timestamp = timestamp;
        this.method = method;
        this.url = url;
        this.userAgent = userAgent;
        this.ip = ip;
    }

}

