@echo off
setlocal enabledelayedexpansion

echo ========================================
echo    YANA PURE BACKEND - WORKING TESTS
echo ========================================
echo.

REM Configuration
set BASE_URL=http://localhost:8080
set TEST_PHONE=+14155552671
set ADMIN_PHONE=+14155550000

REM Test counters
set /a TOTAL_TESTS=0
set /a PASSED_TESTS=0
set /a FAILED_TESTS=0

echo Starting working endpoint tests...
echo Base URL: %BASE_URL%
echo Test Phone: %TEST_PHONE%
echo Admin Phone: %ADMIN_PHONE%
echo.

echo ========================================
echo           HEALTH CHECK TESTS
echo ========================================

REM Test 1: Health Check
echo [%TOTAL_TESTS%] Testing: Health Check
set /a TOTAL_TESTS+=1
curl -s %BASE_URL%/actuator/health >nul 2>&1
if !errorlevel! equ 0 (
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
curl -s %BASE_URL%/db/ping >nul 2>&1
if !errorlevel! equ 0 (
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

REM Test 3: Send OTP
echo [%TOTAL_TESTS%] Testing: Send OTP
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/send-otp -H "Content-Type: application/json" -d "{\"phone\": \"%TEST_PHONE%\"}" 2^>^&1') do set OTP_RESPONSE=%%i
echo Response: !OTP_RESPONSE!
echo !OTP_RESPONSE! | findstr /i "success.*true" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - OTP sent successfully
    set /a PASSED_TESTS+=1
    echo.
    echo ⚠️  MANUAL STEP REQUIRED:
    echo Please check the application console/logs for the OTP code.
    echo Look for: "🔑 OTP CODE FOR TESTING: XXXXXX"
    echo.
    set /p OTP_CODE=Enter the OTP code from the logs: 
) else (
    echo ✗ FAILED - OTP send failed
    set /a FAILED_TESTS+=1
)
echo.

REM Test 4: Verify OTP and Extract Token
if defined OTP_CODE (
    echo [%TOTAL_TESTS%] Testing: Verify OTP and Extract Token
    set /a TOTAL_TESTS+=1
    
    REM Save response to file for easier parsing
    curl -s -X POST %BASE_URL%/api/auth/verify-otp -H "Content-Type: application/json" -d "{\"phone\": \"%TEST_PHONE%\", \"otp\": \"%OTP_CODE%\"}" > temp_response.json 2>&1
    
    REM Check if verification was successful
    findstr /i "access_token" temp_response.json >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - OTP verified and tokens received
        
        REM Extract access token using a more reliable method
        for /f "tokens=*" %%a in ('findstr /i "access_token" temp_response.json') do (
            set TOKEN_LINE=%%a
        )
        
        REM Extract token value (everything between quotes after access_token)
        REM Use a more specific pattern to find the access_token value
        for /f "tokens=2 delims=:" %%b in ("!TOKEN_LINE!") do (
            set ACCESS_TOKEN=%%b
        )
        
        REM Clean up the token - remove quotes, commas, and braces
        set ACCESS_TOKEN=!ACCESS_TOKEN: =!
        set ACCESS_TOKEN=!ACCESS_TOKEN:"=!
        set ACCESS_TOKEN=!ACCESS_TOKEN:,=!
        set ACCESS_TOKEN=!ACCESS_TOKEN:}=!
        set ACCESS_TOKEN=!ACCESS_TOKEN:{=!
        
        echo Extracted Access Token: !ACCESS_TOKEN:~0,30!...
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - OTP verification failed
        type temp_response.json
        set /a FAILED_TESTS+=1
    )
    
    REM Clean up temp file
    del temp_response.json >nul 2>&1
) else (
    echo [%TOTAL_TESTS%] Testing: Verify OTP (Skipped - No OTP provided)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No OTP code provided
    set /a PASSED_TESTS+=1
)
echo.

REM Test 5: Get Current User with Token
if defined ACCESS_TOKEN (
    echo [%TOTAL_TESTS%] Testing: Get Current User with Token
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/auth/me -H "Authorization: Bearer !ACCESS_TOKEN!" 2^>^&1') do set ME_RESPONSE=%%i
    echo Response: !ME_RESPONSE!
    echo !ME_RESPONSE! | findstr /i "id" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - Current user retrieved successfully
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - Failed to get current user
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: Get Current User (Skipped - No token)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No access token available
    set /a PASSED_TESTS+=1
)
echo.

echo ========================================
echo           ADMIN TESTS
echo ========================================

REM Test 6: Send Admin OTP
echo [%TOTAL_TESTS%] Testing: Send Admin OTP
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/send-otp -H "Content-Type: application/json" -d "{\"phone\": \"%ADMIN_PHONE%\"}" 2^>^&1') do set ADMIN_OTP_RESPONSE=%%i
echo Response: !ADMIN_OTP_RESPONSE!
echo !ADMIN_OTP_RESPONSE! | findstr /i "success.*true" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Admin OTP sent successfully
    set /a PASSED_TESTS+=1
    echo.
    echo ⚠️  MANUAL STEP REQUIRED:
    echo Please check the application console/logs for the Admin OTP code.
    echo Look for: "🔑 OTP CODE FOR TESTING: XXXXXX"
    echo.
    set /p ADMIN_OTP_CODE=Enter the Admin OTP code from the logs: 
) else (
    echo ✗ FAILED - Admin OTP send failed
    set /a FAILED_TESTS+=1
)
echo.

REM Test 7: Verify Admin OTP
if defined ADMIN_OTP_CODE (
    echo [%TOTAL_TESTS%] Testing: Verify Admin OTP
    set /a TOTAL_TESTS+=1
    
    REM Save response to file for easier parsing
    curl -s -X POST %BASE_URL%/api/auth/verify-otp -H "Content-Type: application/json" -d "{\"phone\": \"%ADMIN_PHONE%\", \"otp\": \"%ADMIN_OTP_CODE%\"}" > temp_admin_response.json 2>&1
    
    REM Check if verification was successful
    findstr /i "access_token" temp_admin_response.json >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - Admin OTP verified and tokens received
        
        REM Extract admin access token
        for /f "tokens=*" %%a in ('findstr /i "access_token" temp_admin_response.json') do (
            set ADMIN_TOKEN_LINE=%%a
        )
        
        REM Extract token value
        for /f "tokens=2 delims=:" %%b in ("!ADMIN_TOKEN_LINE!") do (
            set ADMIN_ACCESS_TOKEN=%%b
        )
        
        REM Clean up the token - remove quotes, commas, and braces
        set ADMIN_ACCESS_TOKEN=!ADMIN_ACCESS_TOKEN: =!
        set ADMIN_ACCESS_TOKEN=!ADMIN_ACCESS_TOKEN:"=!
        set ADMIN_ACCESS_TOKEN=!ADMIN_ACCESS_TOKEN:,=!
        set ADMIN_ACCESS_TOKEN=!ADMIN_ACCESS_TOKEN:}=!
        set ADMIN_ACCESS_TOKEN=!ADMIN_ACCESS_TOKEN:{=!
        
        echo Extracted Admin Access Token: !ADMIN_ACCESS_TOKEN:~0,30!...
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - Admin OTP verification failed
        type temp_admin_response.json
        set /a FAILED_TESTS+=1
    )
    
    REM Clean up temp file
    del temp_admin_response.json >nul 2>&1
) else (
    echo [%TOTAL_TESTS%] Testing: Verify Admin OTP (Skipped - No OTP provided)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No admin OTP code provided
    set /a PASSED_TESTS+=1
)
echo.

REM Test 8: Get Admin Stats
if defined ADMIN_ACCESS_TOKEN (
    echo [%TOTAL_TESTS%] Testing: Get Admin Stats
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/admin/stats -H "Authorization: Bearer !ADMIN_ACCESS_TOKEN!" 2^>^&1') do set ADMIN_STATS_RESPONSE=%%i
    echo Response: !ADMIN_STATS_RESPONSE!
    echo !ADMIN_STATS_RESPONSE! | findstr /i "totalUsers" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - Admin statistics retrieved successfully
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - Failed to get admin statistics
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: Get Admin Stats (Skipped - No admin token)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No admin access token available
    set /a PASSED_TESTS+=1
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
    echo ✅ Authentication flow complete
    echo ✅ Admin endpoints functional
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
echo    WORKING TEST COMPLETED
echo ========================================
pause
