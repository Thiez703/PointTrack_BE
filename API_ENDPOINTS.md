# API Endpoints

## AttendanceController
- PUT    /api/v1/attendance/{recordId}/admin-update
- POST   /api/v1/attendance/check-in
- POST   /api/v1/attendance/check-out
- GET    /api/v1/attendance/explanations
- POST   /api/v1/attendance/explanations/{id}/approve
- PUT    /api/v1/attendance/explanations/{id}/reject
- POST   /api/v1/attendance/export
- GET    /api/v1/attendance/history
- GET    /api/v1/attendance/locations
- GET    /api/v1/attendance/my-records
- PATCH  /api/v1/attendance/{id}/note
- GET    /api/v1/attendance/records

## AttendanceHistoryController
- POST   /api/v1/attendance-history/export
- GET    /api/v1/attendance-history/history
- GET    /api/v1/attendance-history/locations
- PATCH  /api/v1/attendance-history/{id}/note

## AuthController
- POST   /api/v1/auth/accounts
- POST   /api/v1/auth/login
- POST   /api/v1/auth/logout
- GET    /api/v1/auth/me
- PUT    /api/v1/auth/password/change
- PUT    /api/v1/auth/password/first-change
- POST   /api/v1/auth/password/forgot
- PUT    /api/v1/auth/password/reset
- POST   /api/v1/auth/password/verify-otp
- GET    /api/v1/auth/profile
- PUT    /api/v1/auth/profile
- POST   /api/v1/auth/token/refresh

## CustomerController
- GET    /api/v1/customers
- POST   /api/v1/customers
- GET    /api/v1/customers/{id}
- PUT    /api/v1/customers/{id}
- DELETE /api/v1/customers/{id}
- GET    /api/v1/customers/active-with-gps
- GET    /api/v1/customers/export
- POST   /api/v1/customers/{id}/geocode
- PUT    /api/v1/customers/{id}/gps
- POST   /api/v1/customers/import
- GET    /api/v1/customers/import/template

## EmployeeController
- GET    /api/v1/employees
- POST   /api/v1/employees
- GET    /api/v1/employees/{id}
- PUT    /api/v1/employees/{id}
- DELETE /api/v1/employees/{id}
- POST   /api/v1/employees/import
- GET    /api/v1/employees/import/template
- GET    /api/v1/employees/list-all
- GET    /api/v1/employees/me
- GET    /api/v1/employees/{id}/salary-history
- PATCH  /api/v1/employees/{id}/salary-level
- GET    /api/v1/employees/statistics
- GET    /api/v1/employees/stats
- PATCH  /api/v1/employees/{id}/status

## FileUploadController
- POST   /api/v1/files/upload

## SalaryLevelController
- GET    /api/v1/salary-levels
- POST   /api/v1/salary-levels
- GET    /api/v1/salary-levels/{id}
- PUT    /api/v1/salary-levels/{id}
- DELETE /api/v1/salary-levels/{id}

## SchedulingSettingsController
- GET    /api/v1/scheduling/settings
- PUT    /api/v1/scheduling/settings/grace-period
- PUT    /api/v1/scheduling/settings/penalty-rules
- PUT    /api/v1/scheduling/settings/travel-buffer

## ServicePackageController
- GET    /api/v1/packages
- POST   /api/v1/packages
- GET    /api/v1/packages/{id}
- DELETE /api/v1/packages/{id}/cancel
- PUT    /api/v1/packages/{id}/employee

## ShiftController
- DELETE /api/v1/shifts/{id}
- DELETE /api/v1/shifts/{id}/assign
- DELETE /api/v1/shifts/{id}/hard
- GET    /api/v1/shifts
- GET    /api/v1/shifts/available-employees
- GET    /api/v1/shifts/conflict-check
- GET    /api/v1/shifts/my-today
- GET    /api/v1/shifts/open
- POST   /api/v1/shifts
- POST   /api/v1/shifts/assign
- POST   /api/v1/shifts/{id}/check-in
- POST   /api/v1/shifts/{id}/check-out
- POST   /api/v1/shifts/{id}/claim
- POST   /api/v1/shifts/{id}/confirm
- POST   /api/v1/shifts/copy-week
- POST   /api/v1/shifts/recurring
- PUT    /api/v1/shifts/{id}
- PUT    /api/v1/shifts/{shiftId}/assign

## ShiftSwapController
- PATCH  /api/v1/shift-swap/{id}/approve
- GET    /api/v1/shift-swap/available-employees
- GET    /api/v1/shift-swap/available-shifts
- DELETE /api/v1/shift-swap/{id}
- GET    /api/v1/shift-swap
- POST   /api/v1/shift-swap
- GET    /api/v1/shift-swap/{id}
- PATCH  /api/v1/shift-swap/{id}/reject
- PATCH  /api/v1/shift-swap/{id}/respond
