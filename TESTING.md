# Yana Pure Backend - Endpoint Testing Guide

This guide explains how to use the automated endpoint testing scripts to validate your backend before production deployment.

## 🚀 Quick Start

### Option 1: Interactive Test Runner (Recommended)
```bash
# Run the interactive test runner
run-tests.bat
```

### Option 2: Direct Script Execution
```bash
# Basic tests (no authentication required)
test-endpoints.bat

# Full authentication tests (requires manual OTP input)
test-endpoints-with-auth.bat
```

## 📋 Test Scripts Overview

### 1. `test-endpoints.bat` - Basic Endpoint Tests
**Purpose**: Validates all endpoints without requiring real authentication tokens.

**What it tests**:
- ✅ Health checks (actuator/health, db/ping)
- ✅ Authentication endpoints (send-otp, verify-otp, logout, me)
- ✅ User management endpoints (profile, update)
- ✅ Admin endpoints (create, promote, demote, stats)
- ✅ Error handling (invalid inputs, missing tokens)
- ✅ CORS configuration
- ✅ Input validation
- ✅ Non-existent endpoints (404 handling)

**Best for**:
- CI/CD pipelines
- Quick validation after code changes
- Automated testing
- Production readiness checks

### 2. `test-endpoints-with-auth.bat` - Full Authentication Tests
**Purpose**: Tests complete authentication flow with real tokens.

**What it tests**:
- ✅ Complete OTP authentication flow
- ✅ Token generation and validation
- ✅ Authenticated endpoint access
- ✅ Admin role-based access control
- ✅ Token refresh functionality
- ✅ Logout functionality
- ✅ Authorization boundaries

**Best for**:
- Manual testing before production
- Comprehensive validation
- Testing real user flows
- Debugging authentication issues

### 3. `run-tests.bat` - Interactive Test Runner
**Purpose**: Provides a menu to choose between different testing approaches.

## 🔧 Configuration

### Environment Variables
The test scripts use these default values (can be modified in the scripts):

```bash
BASE_URL=http://localhost:8080
TEST_PHONE=+14155552671
ADMIN_PHONE=+14155550000
TEST_EMAIL=test@example.com
ADMIN_EMAIL=admin@yanapure.com
```

### Prerequisites
1. **Application Running**: Ensure your Spring Boot application is running on `http://localhost:8080`
2. **Database Connected**: PostgreSQL should be running and connected
3. **Admin User Seeded**: The application should have created an initial admin user
4. **Curl Available**: Windows should have curl available (Windows 10+ includes it)

## 📊 Test Results

### Success Criteria
- **All tests pass**: Application is ready for production
- **Health checks pass**: Core infrastructure is working
- **Authentication works**: User flows are functional
- **Authorization works**: Role-based access is properly enforced
- **Error handling works**: Invalid inputs are properly rejected

### Test Output Format
```
[1] Testing: Health Check
✓ PASSED - Status: 200

[2] Testing: Send OTP
✓ PASSED - OTP sent successfully

Total Tests Run: 16
Tests Passed: 16
Tests Failed: 0

🎉 ALL TESTS PASSED! 🎉
```

## 🚨 Troubleshooting

### Common Issues

#### 1. Application Not Running
```
Error: curl: (7) Failed to connect to localhost port 8080
```
**Solution**: Start your Spring Boot application first
```bash
mvn spring-boot:run
```

#### 2. Database Connection Issues
```
Error: Database ping failed
```
**Solution**: Ensure PostgreSQL is running and configured correctly

#### 3. OTP Code Not Found
```
⚠️ MANUAL STEP REQUIRED: Please check the application logs for the OTP code
```
**Solution**: 
1. Check the application console/logs
2. Look for: `🔑 OTP CODE FOR TESTING: XXXXXX`
3. Enter the 6-digit code when prompted

#### 4. Admin User Not Found
```
Error: Admin OTP send failed
```
**Solution**: 
1. Check if admin user was seeded on startup
2. Verify admin phone number in configuration
3. Check application logs for seeding messages

### Debug Mode
To see detailed curl responses, modify the scripts to remove the `-s` flag from curl commands:

```bash
# Change this:
curl -s -X POST %BASE_URL%/api/auth/send-otp

# To this:
curl -X POST %BASE_URL%/api/auth/send-otp
```

## 🔄 Adding New Endpoints

When you add new endpoints, update the test scripts:

### 1. Add to `test-endpoints.bat`
```bash
REM Test X: New Endpoint
echo [%TOTAL_TESTS%] Testing: New Endpoint
set /a TOTAL_TESTS+=1
for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/new-endpoint 2^>^&1') do set NEW_RESPONSE=%%i
echo Response: !NEW_RESPONSE!
echo !NEW_RESPONSE! | findstr /i "expected_response" >nul
if !errorlevel! equ 0 (
    echo ✓ PASSED - New endpoint working
    set /a PASSED_TESTS+=1
) else (
    echo ✗ FAILED - New endpoint not working
    set /a FAILED_TESTS+=1
)
echo.
```

### 2. Add to `test-endpoints-with-auth.bat` (if authenticated)
```bash
REM Test X: New Authenticated Endpoint
if defined ACCESS_TOKEN (
    echo [%TOTAL_TESTS%] Testing: New Authenticated Endpoint
    set /a TOTAL_TESTS+=1
    for /f "tokens=*" %%i in ('curl -s -X GET %BASE_URL%/api/new-endpoint -H "Authorization: Bearer !ACCESS_TOKEN!" 2^>^&1') do set NEW_AUTH_RESPONSE=%%i
    echo Response: !NEW_AUTH_RESPONSE!
    echo !NEW_AUTH_RESPONSE! | findstr /i "expected_response" >nul
    if !errorlevel! equ 0 (
        echo ✓ PASSED - New authenticated endpoint working
        set /a PASSED_TESTS+=1
    ) else (
        echo ✗ FAILED - New authenticated endpoint not working
        set /a FAILED_TESTS+=1
    )
) else (
    echo [%TOTAL_TESTS%] Testing: New Authenticated Endpoint (Skipped - No token)
    set /a TOTAL_TESTS+=1
    echo ⚠️  SKIPPED - No access token available
    set /a PASSED_TESTS+=1
)
echo.
```

## 📈 Production Readiness Checklist

Before deploying to production, ensure:

- [ ] All basic endpoint tests pass (`test-endpoints.bat`)
- [ ] All authentication tests pass (`test-endpoints-with-auth.bat`)
- [ ] Health checks return 200 OK
- [ ] Database connectivity is stable
- [ ] CORS is properly configured
- [ ] Error handling works for all edge cases
- [ ] Input validation rejects invalid data
- [ ] Authorization properly restricts access
- [ ] Admin endpoints require admin role
- [ ] Token refresh functionality works
- [ ] Logout properly invalidates sessions

## 🎯 Best Practices

1. **Run tests before every deployment**
2. **Fix failing tests before proceeding**
3. **Update tests when adding new endpoints**
4. **Use the interactive runner for manual testing**
5. **Use basic tests for automated CI/CD**
6. **Check logs for OTP codes during full auth tests**
7. **Verify admin user seeding on first run**

## 📞 Support

If you encounter issues:
1. Check the troubleshooting section above
2. Verify all prerequisites are met
3. Check application logs for detailed error messages
4. Ensure all environment variables are correctly set
5. Test individual endpoints manually with curl

---

**Happy Testing! 🚀**
