# ResQHeat - Action Items Checklist

## 🔴 Critical (Fix Before Production)

- [ ] **Fix Notification ID Collisions**
  - [ ] Replace `System.currentTimeMillis().toInt()` with proper ID generator
  - [ ] File: `NotificationHelper.kt`
  - [ ] Use `AtomicInteger` or similar thread-safe counter

- [ ] **Implement ViewModels**
  - [ ] Create `HomeViewModel` for HomeScreen
  - [ ] Create `ProfileViewModel` for ProfileScreen
  - [ ] Move business logic from composables to ViewModels
  - [ ] Use StateFlow for state management

- [ ] **Fix Memory Leaks**
  - [ ] Review listener cleanup in HomeScreen
  - [ ] Ensure all Firestore listeners are properly disposed
  - [ ] Add cleanup in ViewModel onCleared()

- [ ] **Add Error Handling**
  - [ ] Create UiState sealed class (Loading, Success, Error)
  - [ ] Show user-friendly error messages
  - [ ] Add retry mechanisms for network failures
  - [ ] Handle Firestore errors gracefully

- [ ] **Input Validation**
  - [ ] Validate phone numbers
  - [ ] Validate coordinates (reject 0.0, 0.0)
  - [ ] Add character limits for text fields
  - [ ] Validate email addresses (for shelters)

- [ ] **Security Review**
  - [ ] Review Firestore security rules
  - [ ] Add rate limiting (consider Firebase App Check)
  - [ ] Validate inputs server-side
  - [ ] Review anonymous auth usage

## ⚠️ High Priority (Next Sprint)

- [ ] **Refactor HomeScreen**
  - [ ] Break into smaller composables
  - [ ] Extract notification logic
  - [ ] Extract map logic
  - [ ] Extract filtering logic
  - [ ] Target: < 300 lines per file

- [ ] **Add Unit Tests**
  - [ ] Test repositories
  - [ ] Test ViewModels (once created)
  - [ ] Test utility functions
  - [ ] Target: 60%+ coverage

- [ ] **Improve Location Handling**
  - [ ] Request permission once on app start
  - [ ] Show rationale dialog
  - [ ] Handle permission denial gracefully
  - [ ] Cache location to avoid repeated requests

- [ ] **Add Offline Support**
  - [ ] Enable Firestore offline persistence
  - [ ] Show offline indicators
  - [ ] Queue actions when offline
  - [ ] Sync when connection restored

- [ ] **Improve Logging**
  - [ ] Integrate Timber or similar
  - [ ] Use appropriate log levels
  - [ ] Remove debug logs in release builds
  - [ ] Add structured logging

- [ ] **Accessibility Improvements**
  - [ ] Add content descriptions to all icons
  - [ ] Test with screen readers
  - [ ] Add accessibility labels
  - [ ] Improve focus management

## 📝 Medium Priority (Future Releases)

- [ ] **Code Quality**
  - [ ] Extract common functions
  - [ ] Create constants file
  - [ ] Move hardcoded strings to resources
  - [ ] Remove code duplication
  - [ ] Improve null safety

- [ ] **Performance Optimization**
  - [ ] Add keys to LazyColumn items
  - [ ] Implement pagination for requests
  - [ ] Debounce map updates
  - [ ] Optimize heatmap calculation
  - [ ] Cache location results

- [ ] **Build Configuration**
  - [ ] Enable ProGuard/R8 in release
  - [ ] Add signing configuration
  - [ ] Create build variants (dev, staging, prod)
  - [ ] Add build configuration docs

- [ ] **Documentation**
  - [ ] Add KDoc to all public functions/classes
  - [ ] Create architecture documentation
  - [ ] Add contributing guidelines
  - [ ] Add API documentation

- [ ] **Testing**
  - [ ] Add integration tests
  - [ ] Add UI tests for critical flows
  - [ ] Set up CI/CD
  - [ ] Add test coverage reporting

## 🚀 Nice to Have (Future Features)

- [ ] **Push Notifications**
  - [ ] Implement Firebase Cloud Messaging (FCM)
  - [ ] Add background notification handling
  - [ ] Add notification preferences

- [ ] **Image Support**
  - [ ] Add image upload for requests
  - [ ] Add Firebase Storage integration
  - [ ] Display images in request details

- [ ] **Search and Filtering**
  - [ ] Add search functionality
  - [ ] Add more filter options
  - [ ] Add sorting options
  - [ ] Add request history view

- [ ] **User Feedback**
  - [ ] Add rating system
  - [ ] Add feedback mechanism
  - [ ] Track NGO performance

- [ ] **Analytics**
  - [ ] Add Firebase Analytics
  - [ ] Track user interactions
  - [ ] Monitor app performance
  - [ ] Track request statistics

## 📊 Metrics to Track

- [ ] Code coverage (target: 60%+)
- [ ] APK size (target: < 50MB)
- [ ] Memory usage (target: < 200MB)
- [ ] Battery usage (target: < 5% per hour)
- [ ] Crash-free rate (target: 99%+)
- [ ] Response time (target: < 2s)

## 🎯 Quick Wins (Do First)

1. Fix notification ID collisions (15 min)
2. Create constants file (30 min)
3. Add input validation (1 hour)
4. Extract common functions (1 hour)
5. Add KDoc comments (2 hours)

---

**Last Updated**: 2024  
**Status**: In Progress

