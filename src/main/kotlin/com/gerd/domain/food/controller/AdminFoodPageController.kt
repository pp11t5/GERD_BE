package com.gerd.domain.food.controller

import com.gerd.domain.food.service.AdminFoodService
import com.gerd.global.apiPayload.GeneralException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.security.Principal

@Controller
class AdminFoodPageController(
    private val adminFoodService: AdminFoodService,
    @Value("\${backoffice.metabase-url:}") private val metabaseUrl: String,
    @Value("\${backoffice.better-stack-url:}") private val betterStackUrl: String,
) {

    @GetMapping("/admin")
    fun index(): String = "redirect:/admin/foods"

    @GetMapping("/admin/login")
    fun login(): String = "admin/login"

    @GetMapping("/admin/foods")
    fun foods(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(required = false) isUnknown: Boolean?,
        principal: Principal,
        model: Model,
    ): String {
        model.addAttribute("foods", adminFoodService.getAllUserFoods(maxOf(page, 0), isUnknown))
        model.addAttribute("isUnknown", isUnknown)
        model.addAttribute("adminName", principal.name)
        model.addAttribute("metabaseUrl", metabaseUrl)
        model.addAttribute("betterStackUrl", betterStackUrl)
        return "admin/foods"
    }

    @PostMapping("/admin/foods/{foodExternalId}/promote")
    fun promote(
        @PathVariable foodExternalId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(required = false) isUnknown: Boolean?,
        redirectAttributes: RedirectAttributes,
    ): String {
        try {
            adminFoodService.promote(foodExternalId)
            redirectAttributes.addFlashAttribute("message", "음식을 승격했습니다.")
        } catch (e: GeneralException) {
            redirectAttributes.addFlashAttribute("error", e.message ?: "승격에 실패했습니다.")
        }
        redirectAttributes.addAttribute("page", page)
        isUnknown?.let { redirectAttributes.addAttribute("isUnknown", it) }
        return "redirect:/admin/foods"
    }
}
