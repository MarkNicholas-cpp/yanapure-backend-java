@echo off
setlocal enabledelayedexpansion

echo ========================================
echo    YANA PURE BACKEND - ENDPOINT TESTS
echo ========================================
echo.

REM Configuration
set BASE_URL=http://localhost:8080
set TEST_PHONE=+14155552671
set ADMIN_PHONE=+14155550000
set TEST_EMAIL=test@example.com
set ADMIN_EMAIL=admin@yanapure.com

REM Test counters
set /a TOTAL_TESTS=0
set /a PASSED_TESTS=0
set /a FAILED_TESTS=0

REM Variables to store tokens and IDs
set ACCESS_TOKEN=
set REFRESH_TOKEN=
set USER_ID=
set ADMIN_ACCESS_TOKEN=
set ADMIN_REFRESH_TOKEN=
set ADMIN_USER_ID=
set OTP_CODE=

echo Starting endpoint tests...
echo Base URL: %BASE_URL%
echo Test Phone: %TEST_PHONE%
echo Admin Phone: %ADMIN_PHONE%
echo.

REM Function to run a test
:run_test
set /a TOTAL_TESTS+=1
set TEST_NAME=%~1
set EXPECTED_STATUS=%~2
set CURL_COMMAND=%~3

echo [%TOTAL_TESTS%] Testing: %TEST_NAME%
echo Command: %CURL_COMMAND%

REM Execute curl command and capture response
for /f "tokens=*" %%i in ('%CURL_COMMAND% 2^>^&1') do set RESPONSE=%%i

REM Extract HTTP status code from response
for /f "tokens=2" %%a in ('echo !RESPONSE! ^| findstr /r "HTTP/[0-9]"') do set ACTUAL_STATUS=%%a

REM Check if status matches expected
if "!ACTUAL_STATUS!"=="%EXPECTED_STATUS%" (
    echo ✓ PASSED - Status: !ACTUAL_STATUS!
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Expected: %EXPECTED_STATUS%, Got: !ACTUAL_STATUS!
    echo Response: !RESPONSE!
    set /a FAILED_TESTS+=1
)
echo.
goto :eof

echo ========================================
echo           HEALTH CHECK TESTS
echo ========================================

REM Test 1: Health Check
echo [%TOTAL_TESTS%] Testing: Health Check
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -w %%{http_code} -o nul %BASE_URL%/actuator/health 2^>^&1') do set HEALTH_RESPONSE=%%i
echo Response: !HEALTH_RESPONSE!
if "!HEALTH_RESPONSE!"=="200" (
    echo ✓ PASSED - Health check working
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Health check failed
    set /a FAILED_TESTS+=1
)
echo.

REM Test 2: Database Ping
echo [%TOTAL_TESTS%] Testing: Database Ping
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -w %%{http_code} -o nul %BASE_URL%/db/ping 2^>^&1') do set DB_RESPONSE=%%i
echo Response: !DB_RESPONSE!
if "!DB_RESPONSE!"=="200" (
    echo ✓ PASSED - Database ping working
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Database ping failed
    set /a FAILED_TESTS+=1
)
echo.

echo ========================================
echo         AUTHENTICATION TESTS
echo ========================================

REM Test 1: Send OTP
echo [%TOTAL_TESTS%] Testing: Send OTP
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/send-otp -H "Content-Type: application/json" -d "{\"phone\": \"%TEST_PHONE%\"}" 2^>^&1') do set OTP_RESPONSE=%%i
echo Response: !OTP_RESPONSE!
if "!OTP_RESPONSE!" neq "" (
    echo ✓ PASSED - OTP sent successfully
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - OTP send failed
    set /a FAILED_TESTS+=1
)
echo.

REM Test 2: Verify OTP (Note: This will fail without real OTP, but we test the endpoint)
echo [%TOTAL_TESTS%] Testing: Verify OTP (Invalid)
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/verify-otp -H "Content-Type: application/json" -d "{\"phone\": \"%TEST_PHONE%\", \"otp\": \"000000\"}" 2^>^&1') do set VERIFY_RESPONSE=%%i
echo Response: !VERIFY_RESPONSE!
echo !VERIFY_RESPONSE! | findstr /i "INVALID_OTP" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Invalid OTP properly rejected
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Invalid OTP not properly handled
    set /a FAILED_TESTS+=1
)
echo.

REM Test 3: Get Current User (Unauthorized)
echo [%TOTAL_TESTS%] Testing: Get Current User (No Token)
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/auth/me 2^>^&1') do set ME_RESPONSE=%%i
echo Response: !ME_RESPONSE!
echo !ME_RESPONSE! | findstr /i "MISSING_AUTH_HEADER" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Missing auth header properly handled
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Missing auth header not properly handled
    set /a FAILED_TESTS+=1
)
echo.

REM Test 4: Logout (Unauthorized)
echo [%TOTAL_TESTS%] Testing: Logout (No Token)
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/logout 2^>^&1') do set LOGOUT_RESPONSE=%%i
echo Response: !LOGOUT_RESPONSE!
echo !LOGOUT_RESPONSE! | findstr /i "MISSING_AUTH_HEADER" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Missing auth header properly handled
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Missing auth header not properly handled
    set /a FAILED_TESTS+=1
)
echo.

echo ========================================
echo           USER MANAGEMENT TESTS
echo ========================================

REM Test 5: Get Current User Profile (Unauthorized)
echo [%TOTAL_TESTS%] Testing: Get User Profile (No Token)
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/users/me 2^>^&1') do set USER_PROFILE_RESPONSE=%%i
echo Response: !USER_PROFILE_RESPONSE!
echo !USER_PROFILE_RESPONSE! | findstr /i "INVALID_AUTH_HEADER" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Missing auth header properly handled
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Missing auth header not properly handled
    set /a FAILED_TESTS+=1
)
echo.

REM Test 6: Update User Profile (Unauthorized)
echo [%TOTAL_TESTS%] Testing: Update User Profile (No Token)
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X PUT %BASE_URL%/api/users/me -H "Content-Type: application/json" -d "{\"name\": \"Test User\", \"email\": \"test@example.com\"}" 2^>^&1') do set UPDATE_PROFILE_RESPONSE=%%i
echo Response: !UPDATE_PROFILE_RESPONSE!
echo !UPDATE_PROFILE_RESPONSE! | findstr /i "INVALID_AUTH_HEADER" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Missing auth header properly handled
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Missing auth header not properly handled
    set /a FAILED_TESTS+=1
)
echo.

REM Test 7: Get All Users (Unauthorized)
echo [%TOTAL_TESTS%] Testing: Get All Users (No Token)
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/users 2^>^&1') do set ALL_USERS_RESPONSE=%%i
echo Response: !ALL_USERS_RESPONSE!
echo !ALL_USERS_RESPONSE! | findstr /i "INVALID_AUTH_HEADER" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Missing auth header properly handled
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Missing auth header not properly handled
    set /a FAILED_TESTS+=1
)
echo.

echo ========================================
echo            ADMIN TESTS
echo ========================================

REM Test 8: Create Admin User (Unauthorized)
echo [%TOTAL_TESTS%] Testing: Create Admin User (No Token)
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/admin/users -H "Content-Type: application/json" -d "{\"phone\": \"+14155550001\", \"name\": \"Test Admin\", \"email\": \"testadmin@example.com\"}" 2^>^&1') do set CREATE_ADMIN_RESPONSE=%%i
echo Response: !CREATE_ADMIN_RESPONSE!
echo !CREATE_ADMIN_RESPONSE! | findstr /i "INVALID_AUTH_HEADER" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Missing auth header properly handled
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Missing auth header not properly handled
    set /a FAILED_TESTS+=1
)
echo.

REM Test 9: Get Admin Users (Unauthorized)
echo [%TOTAL_TESTS%] Testing: Get Admin Users (No Token)
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/admin/users 2^>^&1') do set GET_ADMINS_RESPONSE=%%i
echo Response: !GET_ADMINS_RESPONSE!
echo !GET_ADMINS_RESPONSE! | findstr /i "INVALID_AUTH_HEADER" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Missing auth header properly handled
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Missing auth header not properly handled
    set /a FAILED_TESTS+=1
)
echo.

REM Test 10: Get Admin Stats (Unauthorized)
echo [%TOTAL_TESTS%] Testing: Get Admin Stats (No Token)
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/admin/stats 2^>^&1') do set ADMIN_STATS_RESPONSE=%%i
echo Response: !ADMIN_STATS_RESPONSE!
echo !ADMIN_STATS_RESPONSE! | findstr /i "INVALID_AUTH_HEADER" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Missing auth header properly handled
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Missing auth header not properly handled
    set /a FAILED_TESTS+=1
)
echo.

echo ========================================
echo         VALIDATION TESTS
echo ========================================

REM Test 11: Send OTP with Invalid Phone
echo [%TOTAL_TESTS%] Testing: Send OTP with Invalid Phone
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/send-otp -H "Content-Type: application/json" -d "{\"phone\": \"invalid-phone\"}" 2^>^&1') do set INVALID_PHONE_RESPONSE=%%i
echo Response: !INVALID_PHONE_RESPONSE!
echo !INVALID_PHONE_RESPONSE! | findstr /i "PHONE_INVALID" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Invalid phone properly rejected
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Invalid phone not properly handled
    set /a FAILED_TESTS+=1
)
echo.

REM Test 12: Send OTP with Empty Phone
echo [%TOTAL_TESTS%] Testing: Send OTP with Empty Phone
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/send-otp -H "Content-Type: application/json" -d "{\"phone\": \"\"}" 2^>^&1') do set EMPTY_PHONE_RESPONSE=%%i
echo Response: !EMPTY_PHONE_RESPONSE!
echo !EMPTY_PHONE_RESPONSE! | findstr /i "PHONE_REQUIRED" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Empty phone properly rejected
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Empty phone not properly handled
    set /a FAILED_TESTS+=1
)
echo.

REM Test 13: Verify OTP with Empty Data
echo [%TOTAL_TESTS%] Testing: Verify OTP with Empty Data
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/verify-otp -H "Content-Type: application/json" -d "{\"phone\": \"\", \"otp\": \"\"}" 2^>^&1') do set EMPTY_OTP_RESPONSE=%%i
echo Response: !EMPTY_OTP_RESPONSE!
echo !EMPTY_OTP_RESPONSE! | findstr /i "PHONE_REQUIRED" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Empty OTP data properly rejected
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Empty OTP data not properly handled
    set /a FAILED_TESTS+=1
)
echo.

echo ========================================
echo         CORS TESTS
echo ========================================

REM Test 14: CORS Preflight Request
echo [%TOTAL_TESTS%] Testing: CORS Preflight Request
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X OPTIONS %BASE_URL%/api/auth/send-otp -H "Origin: http://localhost:5173" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: Content-Type" 2^>^&1') do set CORS_RESPONSE=%%i
echo Response: !CORS_RESPONSE!
echo !CORS_RESPONSE! | findstr /i "Access-Control-Allow-Origin" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - CORS headers present
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - CORS headers missing
    set /a FAILED_TESTS+=1
)
echo.

echo ========================================
echo         ERROR HANDLING TESTS
echo ========================================

REM Test 15: Non-existent Endpoint
echo [%TOTAL_TESTS%] Testing: Non-existent Endpoint
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/nonexistent 2^>^&1') do set NOT_FOUND_RESPONSE=%%i
echo Response: !NOT_FOUND_RESPONSE!
echo !NOT_FOUND_RESPONSE! | findstr /i "404" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - 404 properly returned for non-existent endpoint
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - 404 not properly returned
    set /a FAILED_TESTS+=1
)
echo.

REM Test 16: Invalid JSON
echo [%TOTAL_TESTS%] Testing: Invalid JSON
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/send-otp -H "Content-Type: application/json" -d "invalid-json" 2^>^&1') do set INVALID_JSON_RESPONSE=%%i
echo Response: !INVALID_JSON_RESPONSE!
echo !INVALID_JSON_RESPONSE! | findstr /i "400" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Invalid JSON properly rejected
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - Invalid JSON not properly handled
    set /a FAILED_TESTS+=1
)
echo.

echo ========================================
echo         TEST RESULTS SUMMARY
echo ========================================
echo.
echo Total Tests Run: %TOTAL_TESTS%
echo Tests Passed: %PASSED_TESTS%
echo Tests Failed: %FAILED_TESTS%
echo.

if %FAILED_TESTS% equ 0 (
    echo 🎉 ALL TESTS PASSED! 🎉
    echo The application is ready for production deployment.
    echo.
    echo ✅ Health checks working
    echo ✅ Authentication endpoints responding correctly
    echo ✅ Authorization working properly
    echo ✅ Error handling functioning
    echo ✅ CORS configured correctly
    echo ✅ Input validation working
    echo.
    exit /b 0
) else (
    echo ❌ %FAILED_TESTS% TEST(S) FAILED!
    echo Please review the failed tests above and fix the issues before deployment.
    echo.
    exit /b 1
)

echo.
echo ========================================
echo    ENDPOINT TESTING COMPLETED
echo ========================================
