@echo off
cd /d "%~dp0backend"
call mvn.cmd spring-boot:run
