package com.freightfox.tollplaza.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.Hidden;

/**
 * Sends visitors of the bare deployed URL to the Swagger UI.
 */
@Hidden
@Controller
public class RootController {

	@GetMapping("/")
	public String root() {
		return "redirect:/swagger-ui.html";
	}
}
