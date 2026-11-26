package com.gerd.domain.admin.repository

import com.gerd.domain.admin.entity.AdminActionLog
import org.springframework.data.jpa.repository.JpaRepository

interface AdminActionLogRepository : JpaRepository<AdminActionLog, Long>
