@echo off
echo ========================================
echo    YANA PURE BACKEND - TEST RUNNER
echo ========================================
echo.
echo Choose your testing approach:
echo.
echo 1. Basic Endpoint Tests (No Authentication Required)
echo    - Tests all endpoints without requiring real tokens
echo    - Validates error handling, CORS, and basic functionality
echo    - Good for CI/CD and quick validation
echo.
echo 2. Full Authentication Tests (Requires Manual OTP Input)
echo    - Tests complete authentication flow
echo    - Requires manual OTP code entry from logs
echo    - Tests all authenticated endpoints
echo    - More comprehensive but requires interaction
echo.
echo 3. Run Both Test Suites
echo.
echo 4. Exit
echo.
set /p choice=Enter your choice (1-4): 

if "%choice%"=="1" (
    echo.
    echo Running Basic Endpoint Tests...
    echo ========================================
    call test-endpoints.bat
) else if "%choice%"=="2" (
    echo.
    echo Running Full Authentication Tests...
    echo ========================================
    call test-endpoints-with-auth.bat
) else if "%choice%"=="3" (
    echo.
    echo Running Both Test Suites...
    echo ========================================
    echo.
    echo === BASIC ENDPOINT TESTS ===
    call test-endpoints.bat
    echo.
    echo === FULL AUTHENTICATION TESTS ===
    call test-endpoints-with-auth.bat
) else if "%choice%"=="4" (
    echo Exiting...
    exit /b 0
) else (
    echo Invalid choice. Please run the script again.
    exit /b 1
)

echo.
echo ========================================
echo    TEST RUNNER COMPLETED
echo ========================================
pause
