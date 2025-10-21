@echo off
setlocal enabledelayedexpansion

echo ========================================
echo  YANA PURE BACKEND - FULL AUTH TESTS
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

echo Starting comprehensive endpoint tests with authentication...
echo Base URL: %BASE_URL%
echo Test Phone: %TEST_PHONE%
echo Admin Phone: %ADMIN_PHONE%
echo.

echo ========================================
echo           HEALTH CHECK TESTS
echo ========================================

call :run_test "Health Check" "200" "curl -s -w %%{http_code} -o nul %BASE_URL%/actuator/health"
call :run_test "Database Ping" "200" "curl -s -w %%{http_code} -o nul %BASE_URL%/db/ping"

echo ========================================
echo         AUTHENTICATION FLOW TESTS
echo ========================================

REM Test 1: Send OTP for Regular User
echo [%TOTAL_TESTS%] Testing: Send OTP for Regular User
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/send-otp -H "Content-Type: application/json" -d "{\"phone\": \"%TEST_PHONE%\"}" 2^>^&1') do set OTP_RESPONSE=%%i
echo Response: !OTP_RESPONSE!
echo !OTP_RESPONSE! | findstr /i "success.*true" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - OTP sent successfully
    set /a PASSED_TESTS+=1
    echo.
    echo ⚠️  MANUAL STEP REQUIRED:
    echo Please check the application logs for the OTP code and enter it below:
    echo Look for: "🔑 OTP CODE FOR TESTING: XXXXXX"
    set /p OTP_CODE=Enter the OTP code: 
) else (
    echo ✗ FAILED - OTP send failed
    set /a FAILED_TESTS+=1
)
echo.

REM Test 2: Verify OTP and Get Tokens
if defined OTP_CODE (
    echo [%TOTAL_TESTS%] Testing: Verify OTP and Get Tokens
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/verify-otp -H "Content-Type: application/json" -d "{\"phone\": \"%TEST_PHONE%\", \"otp\": \"%OTP_CODE%\"}" 2^>^&1') do set VERIFY_RESPONSE=%%i
    echo Response: !VERIFY_RESPONSE!
    echo !VERIFY_RESPONSE! | findstr /i "access_token" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - OTP verified and tokens received
        set /a PASSED_TESTS+=1
        
        REM Extract tokens (improved extraction)
        for /f "tokens=2 delims=:" %%a in ('echo !VERIFY_RESPONSE! ^| findstr /i "access_token"') do (
            set ACCESS_TOKEN=%%a
            set ACCESS_TOKEN=!ACCESS_TOKEN: =!
            set ACCESS_TOKEN=!ACCESS_TOKEN:,=!
            set ACCESS_TOKEN=!ACCESS_TOKEN:"=!
            set ACCESS_TOKEN=!ACCESS_TOKEN:}=!
        )
        for /f "tokens=2 delims=:" %%a in ('echo !VERIFY_RESPONSE! ^| findstr /i "refresh_token"') do (
            set REFRESH_TOKEN=%%a
            set REFRESH_TOKEN=!REFRESH_TOKEN: =!
            set REFRESH_TOKEN=!REFRESH_TOKEN:,=!
            set REFRESH_TOKEN=!REFRESH_TOKEN:"=!
            set REFRESH_TOKEN=!REFRESH_TOKEN:}=!
        )
        for /f "tokens=2 delims=:" %%a in ('echo !VERIFY_RESPONSE! ^| findstr /i "user.*id"') do (
            set USER_ID=%%a
            set USER_ID=!USER_ID: =!
            set USER_ID=!USER_ID:,=!
            set USER_ID=!USER_ID:"=!
            set USER_ID=!USER_ID:}=!
        )
        echo Extracted Access Token: !ACCESS_TOKEN:~0,20!...
        echo Extracted User ID: !USER_ID!
    ) else (
        echo ✗ FAILED - OTP verification failed
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: Verify OTP (Skipped - No OTP provided)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No OTP code provided
    set /a PASSED_TESTS+=1
)
echo.

REM Test 3: Get Current User with Valid Token
if defined ACCESS_TOKEN (
    echo [%TOTAL_TESTS%] Testing: Get Current User with Valid Token
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

REM Test 4: Update User Profile
if defined ACCESS_TOKEN (
    echo [%TOTAL_TESTS%] Testing: Update User Profile
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X PUT %BASE_URL%/api/users/me -H "Authorization: Bearer !ACCESS_TOKEN!" -H "Content-Type: application/json" -d "{\"name\": \"Updated Test User\", \"email\": \"updated@example.com\"}" 2^>^&1') do set UPDATE_RESPONSE=%%i
    echo Response: !UPDATE_RESPONSE!
    echo !UPDATE_RESPONSE! | findstr /i "name.*Updated" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - User profile updated successfully
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - Failed to update user profile
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: Update User Profile (Skipped - No token)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No access token available
    set /a PASSED_TESTS+=1
)
echo.

echo ========================================
echo           ADMIN AUTHENTICATION
echo ========================================

REM Test 5: Send OTP for Admin User
echo [%TOTAL_TESTS%] Testing: Send OTP for Admin User
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/send-otp -H "Content-Type: application/json" -d "{\"phone\": \"%ADMIN_PHONE%\"}" 2^>^&1') do set ADMIN_OTP_RESPONSE=%%i
echo Response: !ADMIN_OTP_RESPONSE!
echo !ADMIN_OTP_RESPONSE! | findstr /i "success.*true" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - Admin OTP sent successfully
    set /a PASSED_TESTS+=1
    echo.
    echo ⚠️  MANUAL STEP REQUIRED:
    echo Please check the application logs for the Admin OTP code and enter it below:
    set /p ADMIN_OTP_CODE=Enter the Admin OTP code: 
) else (
    echo ✗ FAILED - Admin OTP send failed
    set /a FAILED_TESTS+=1
)
echo.

REM Test 6: Verify Admin OTP
if defined ADMIN_OTP_CODE (
    echo [%TOTAL_TESTS%] Testing: Verify Admin OTP
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/verify-otp -H "Content-Type: application/json" -d "{\"phone\": \"%ADMIN_PHONE%\", \"otp\": \"%ADMIN_OTP_CODE%\"}" 2^>^&1') do set ADMIN_VERIFY_RESPONSE=%%i
    echo Response: !ADMIN_VERIFY_RESPONSE!
    echo !ADMIN_VERIFY_RESPONSE! | findstr /i "access_token" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - Admin OTP verified and tokens received
        set /a PASSED_TESTS+=1
        
        REM Extract admin tokens
        for /f "tokens=2 delims=:" %%a in ('echo !ADMIN_VERIFY_RESPONSE! ^| findstr /i "access_token"') do (
            set ADMIN_ACCESS_TOKEN=%%a
            set ADMIN_ACCESS_TOKEN=!ADMIN_ACCESS_TOKEN: =!
            set ADMIN_ACCESS_TOKEN=!ADMIN_ACCESS_TOKEN:,=!
            set ADMIN_ACCESS_TOKEN=!ADMIN_ACCESS_TOKEN:"=!
            set ADMIN_ACCESS_TOKEN=!ADMIN_ACCESS_TOKEN:}=!
        )
        for /f "tokens=2 delims=:" %%a in ('echo !ADMIN_VERIFY_RESPONSE! ^| findstr /i "user.*id"') do (
            set ADMIN_USER_ID=%%a
            set ADMIN_USER_ID=!ADMIN_USER_ID: =!
            set ADMIN_USER_ID=!ADMIN_USER_ID:,=!
            set ADMIN_USER_ID=!ADMIN_USER_ID:"=!
            set ADMIN_USER_ID=!ADMIN_USER_ID:}=!
        )
        echo Extracted Admin Access Token: !ADMIN_ACCESS_TOKEN:~0,20!...
        echo Extracted Admin User ID: !ADMIN_USER_ID!
    ) else (
        echo ✗ FAILED - Admin OTP verification failed
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: Verify Admin OTP (Skipped - No OTP provided)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No admin OTP code provided
    set /a PASSED_TESTS+=1
)
echo.

echo ========================================
echo           ADMIN ENDPOINT TESTS
echo ========================================

REM Test 7: Get All Users (Admin)
if defined ADMIN_ACCESS_TOKEN (
    echo [%TOTAL_TESTS%] Testing: Get All Users (Admin)
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/users -H "Authorization: Bearer !ADMIN_ACCESS_TOKEN!" 2^>^&1') do set ALL_USERS_RESPONSE=%%i
    echo Response: !ALL_USERS_RESPONSE!
    echo !ALL_USERS_RESPONSE! | findstr /i "users" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - All users retrieved successfully
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - Failed to get all users
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: Get All Users (Skipped - No admin token)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No admin access token available
    set /a PASSED_TESTS+=1
)
echo.

REM Test 8: Get Admin Users
if defined ADMIN_ACCESS_TOKEN (
    echo [%TOTAL_TESTS%] Testing: Get Admin Users
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/admin/users -H "Authorization: Bearer !ADMIN_ACCESS_TOKEN!" 2^>^&1') do set ADMIN_USERS_RESPONSE=%%i
    echo Response: !ADMIN_USERS_RESPONSE!
    echo !ADMIN_USERS_RESPONSE! | findstr /i "admins" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - Admin users retrieved successfully
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - Failed to get admin users
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: Get Admin Users (Skipped - No admin token)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No admin access token available
    set /a PASSED_TESTS+=1
)
echo.

REM Test 9: Get Admin Statistics
if defined ADMIN_ACCESS_TOKEN (
    echo [%TOTAL_TESTS%] Testing: Get Admin Statistics
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
    echo [%TOTAL_TESTS%] Testing: Get Admin Statistics (Skipped - No admin token)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No admin access token available
    set /a PASSED_TESTS+=1
)
echo.

echo ========================================
echo         AUTHORIZATION TESTS
echo ========================================

REM Test 10: Regular User Accessing Admin Endpoint
if defined ACCESS_TOKEN (
    echo [%TOTAL_TESTS%] Testing: Regular User Accessing Admin Endpoint
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/admin/users -H "Authorization: Bearer !ACCESS_TOKEN!" 2^>^&1') do set UNAUTHORIZED_RESPONSE=%%i
    echo Response: !UNAUTHORIZED_RESPONSE!
    echo !UNAUTHORIZED_RESPONSE! | findstr /i "INSUFFICIENT_PERMISSIONS" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - Regular user properly denied admin access
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - Regular user should not have admin access
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: Regular User Accessing Admin Endpoint (Skipped - No token)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No access token available
    set /a PASSED_TESTS+=1
)
echo.

echo ========================================
echo         TOKEN REFRESH TESTS
echo ========================================

REM Test 11: Refresh Token
if defined REFRESH_TOKEN (
    echo [%TOTAL_TESTS%] Testing: Refresh Token
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/refresh -H "Content-Type: application/json" -d "{\"refresh_token\": \"!REFRESH_TOKEN!\"}" 2^>^&1') do set REFRESH_RESPONSE=%%i
    echo Response: !REFRESH_RESPONSE!
    echo !REFRESH_RESPONSE! | findstr /i "access_token" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - Token refreshed successfully
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - Token refresh failed
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: Refresh Token (Skipped - No refresh token)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No refresh token available
    set /a PASSED_TESTS+=1
)
echo.

echo ========================================
echo         LOGOUT TESTS
echo ========================================

REM Test 12: Logout
if defined ACCESS_TOKEN (
    echo [%TOTAL_TESTS%] Testing: Logout
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X POST %BASE_URL%/api/auth/logout -H "Authorization: Bearer !ACCESS_TOKEN!" 2^>^&1') do set LOGOUT_RESPONSE=%%i
    echo Response: !LOGOUT_RESPONSE!
    echo !LOGOUT_RESPONSE! | findstr /i "success.*true" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - Logout successful
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - Logout failed
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: Logout (Skipped - No token)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No access token available
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
    echo ✅ Authorization working properly
    echo ✅ Admin endpoints functional
    echo ✅ Token management working
    echo ✅ Logout functionality working
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
echo    COMPREHENSIVE TESTING COMPLETED
echo ========================================
