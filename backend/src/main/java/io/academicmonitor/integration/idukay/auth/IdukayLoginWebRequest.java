package io.academicmonitor.integration.idukay.auth;

record IdukayLoginWebRequest(String email, String password, String subdomain_school) {}
