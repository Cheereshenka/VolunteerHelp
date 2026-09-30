package ru.coursework.volunteerhelp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.coursework.volunteerhelp.domain.ServiceRequest;
import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    Page<ServiceRequest> findByCustomerId(Long customerId, Pageable pageable);
    List<ServiceRequest> findByCustomerIdAndStatus(Long customerId, ServiceRequest.Status status);
    Page<ServiceRequest> findByStatus(ServiceRequest.Status status, Pageable pageable);
}
