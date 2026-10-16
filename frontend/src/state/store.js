/**
 * RailConnect Reactive Application Store
 * Simple, robust pub/sub state manager for UI components.
 */

// Helper for default tomorrow date string (YYYY-MM-DD)
function getDefaultTomorrowDate() {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return tomorrow.toISOString().split('T')[0];
}

function getStoredUser() {
  try {
    const raw = localStorage.getItem('rc_user');
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

class Store {
  constructor() {
    this.state = {
      currentUser: getStoredUser(),
      token: localStorage.getItem('rc_token') || null,
      selectedQuota: 'GENERAL', // 'GENERAL' | 'TATKAL'
      searchParams: {
        from: 'PNBE',
        fromName: 'Patna Junction',
        to: 'NDLS',
        toName: 'New Delhi',
        date: getDefaultTomorrowDate(),
      },
      searchResults: [],
      liveAvailabilities: {}, // trainNumber -> TrainAvailabilityResponse
      infrastructureStatus: {
        mysql: 'CHECKING',
        mongodb: 'CHECKING',
        redis: 'CHECKING',
        kafka: 'CHECKING',
      },
      activeTab: 'search', // 'search' | 'pnr' | 'bookings' | 'masterList'
      activeModal: null, // null | 'auth' | 'route' | 'coachPicker' | 'booking' | 'ticket'
      activeRouteTrain: null,
      activeCoachPicker: null,
      activeBookingData: null,
      activeTicket: null,
      notificationDrawerOpen: false,
      lockedSeats: [], // [ { trainNumber, journeyDate, classCode, coachNumber, seatNumber, berthType, expiresAt } ]
      authTab: 'login', // 'login' | 'register'
      isSearching: false,
      searchError: null,
      notifications: [],
    };

    this.listeners = new Set();
  }

  getState() {
    return this.state;
  }

  setState(partial) {
    this.state = { ...this.state, ...partial };
    this.notify();
  }

  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  notify() {
    for (const listener of this.listeners) {
      listener(this.state);
    }
  }

  // Convenience action helpers
  setQuota(quota) {
    this.setState({ selectedQuota: quota });
  }

  setSearchParams(params) {
    this.setState({ searchParams: { ...this.state.searchParams, ...params } });
  }

  setSearchResults(results) {
    this.setState({ searchResults: results, isSearching: false, searchError: null });
  }

  setSearching(isSearching) {
    this.setState({ isSearching });
  }

  setSearchError(err) {
    this.setState({ searchError: err, isSearching: false });
  }

  setLiveAvailability(trainNumber, availability) {
    this.setState({
      liveAvailabilities: {
        ...this.state.liveAvailabilities,
        [trainNumber]: availability,
      },
    });
  }

  setInfrastructureStatus(status) {
    this.setState({ infrastructureStatus: status });
  }

  setActiveTab(tab) {
    this.setState({ activeTab: tab });
  }

  openModal(modalName, payload = null) {
    if (modalName === 'route') {
      this.setState({ activeModal: modalName, activeRouteTrain: payload });
    } else if (modalName === 'coachPicker') {
      this.setState({ activeModal: modalName, activeCoachPicker: payload });
    } else if (modalName === 'booking') {
      this.setState({ activeModal: modalName, activeBookingData: payload });
    } else if (modalName === 'ticket') {
      this.setState({ activeModal: modalName, activeTicket: payload });
    } else {
      this.setState({ activeModal: modalName });
    }
  }

  closeModal() {
    this.setState({
      activeModal: null,
      activeRouteTrain: null,
      activeCoachPicker: null,
      activeBookingData: null,
      activeTicket: null,
    });
  }

  openCoachPicker(coachPickerData) {
    this.openModal('coachPicker', coachPickerData);
  }

  openBookingModal(bookingData) {
    this.openModal('booking', bookingData);
  }

  openTicketModal(ticket) {
    this.openModal('ticket', ticket);
  }

  toggleNotificationDrawer(forceOpen = null) {
    this.setState({
      notificationDrawerOpen: forceOpen !== null ? forceOpen : !this.state.notificationDrawerOpen,
    });
  }

  setLockedSeats(lockedSeats) {
    this.setState({ lockedSeats });
  }

  addLockedSeat(seat) {
    const existing = this.state.lockedSeats.filter(
      s => !(s.coachNumber === seat.coachNumber && s.seatNumber === seat.seatNumber)
    );
    this.setState({ lockedSeats: [...existing, seat] });
  }

  removeLockedSeat(coachNumber, seatNumber) {
    this.setState({
      lockedSeats: this.state.lockedSeats.filter(
        s => !(s.coachNumber === coachNumber && s.seatNumber === seatNumber)
      ),
    });
  }

  clearLockedSeats() {
    this.setState({ lockedSeats: [] });
  }

  setAuthTab(tab) {
    this.setState({ authTab: tab });
  }

  setAuth(user, token) {
    localStorage.setItem('rc_user', JSON.stringify(user));
    localStorage.setItem('rc_token', token);
    this.setState({ currentUser: user, token, activeModal: null });
  }

  logout() {
    localStorage.removeItem('rc_user');
    localStorage.removeItem('rc_token');
    this.setState({ currentUser: null, token: null });
  }

  setNotifications(notifications) {
    this.setState({ notifications });
  }
}

export const store = new Store();
