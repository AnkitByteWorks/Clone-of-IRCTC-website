/**
 * Centralized RailConnect API Client
 * Automatically manages JWT bearer tokens, unwraps ApiResponse<T>,
 * and provides typed API interfaces across microservices.
 */

export async function apiRequest(path, options = {}) {
  const token = localStorage.getItem('rc_token');
  const headers = {
    'Accept': 'application/json',
    ...(options.headers || {}),
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  let body = options.body;
  if (body && typeof body === 'object' && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }

  const response = await fetch(path, {
    ...options,
    headers,
    body,
  });

  let json;
  try {
    json = await response.json();
  } catch {
    throw new Error(`HTTP ${response.status}: Failed to parse server response`);
  }

  if (!response.ok || !json.success) {
    const errorMsg = json.message || `Request failed with status ${response.status}`;
    const err = new Error(errorMsg);
    err.status = response.status;
    err.errorCode = json.errorCode;
    throw err;
  }

  return json.data;
}

export const SystemApi = {
  getStatus: () => apiRequest('/api/v1/system/status'),
};

export const CatalogApi = {
  searchStations: (query) => 
    apiRequest(`/api/v1/catalog/stations/search?q=${encodeURIComponent(query)}`),

  searchTrains: (from, to, date) => 
    apiRequest(`/api/v1/catalog/trains/search?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}&date=${encodeURIComponent(date)}`),

  getTrain: (trainNumber) => 
    apiRequest(`/api/v1/catalog/trains/${encodeURIComponent(trainNumber)}`),
};

export const AuthApi = {
  login: ({ email, password }) => 
    apiRequest('/api/v1/auth/login', {
      method: 'POST',
      body: { email, password },
    }),

  register: ({ fullName, email, phoneNumber, password }) => 
    apiRequest('/api/v1/auth/register', {
      method: 'POST',
      body: { fullName, email, phoneNumber, password },
    }),
};

export const UserApi = {
  getSavedPassengers: () => 
    apiRequest('/api/v1/users/me/passengers'),

  addSavedPassenger: (passenger) => 
    apiRequest('/api/v1/users/me/passengers', {
      method: 'POST',
      body: passenger,
    }),

  deleteSavedPassenger: (passengerId) => 
    apiRequest(`/api/v1/users/me/passengers/${passengerId}`, {
      method: 'DELETE',
    }),
};

export const InventoryApi = {
  getTrainAvailability: (trainNumber, date) => 
    apiRequest(`/api/v1/inventory/trains/${encodeURIComponent(trainNumber)}/availability?date=${encodeURIComponent(date)}`),

  getSeatMap: (trainNumber, date, classCode) => 
    apiRequest(`/api/v1/inventory/trains/${encodeURIComponent(trainNumber)}/seats?date=${encodeURIComponent(date)}&classCode=${encodeURIComponent(classCode)}`),

  getSeats: (trainNumber, date, classCode) =>
    apiRequest(`/api/v1/inventory/trains/${encodeURIComponent(trainNumber)}/seats?date=${encodeURIComponent(date)}&classCode=${encodeURIComponent(classCode)}`),

  lockSeat: (trainNumber, journeyDate, classCode, coachNumber, seatNumber) =>
    apiRequest('/api/v1/inventory/seats/lock', {
      method: 'POST',
      body: { trainNumber, journeyDate, classCode, coachNumber, seatNumber },
    }),

  unlockSeat: (trainNumber, journeyDate, classCode, coachNumber, seatNumber) =>
    apiRequest('/api/v1/inventory/seats/unlock', {
      method: 'POST',
      body: { trainNumber, journeyDate, classCode, coachNumber, seatNumber },
    }),
};

export const BookingApi = {
  createBooking: (bookingData) => 
    apiRequest('/api/v1/bookings', {
      method: 'POST',
      body: bookingData,
    }),

  getByPnr: (pnr) => 
    apiRequest(`/api/v1/bookings/pnr/${encodeURIComponent(pnr)}`),

  getMyBookings: () => 
    apiRequest('/api/v1/bookings/my'),

  cancelBooking: (pnr) => 
    apiRequest(`/api/v1/bookings/${encodeURIComponent(pnr)}/cancel`, {
      method: 'POST',
    }),

  downloadTicketPdf: async (pnr) => {
    const token = localStorage.getItem('rc_token');
    const headers = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    const response = await fetch(`/api/v1/bookings/pnr/${encodeURIComponent(pnr)}/pdf`, {
      headers,
    });
    if (!response.ok) {
      let errorMsg = `HTTP ${response.status}: Failed to download ticket PDF`;
      try {
        const errJson = await response.json();
        if (errJson && errJson.message) errorMsg = errJson.message;
      } catch {
        // ignore
      }
      throw new Error(errorMsg);
    }
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `RailConnect_Ticket_${pnr}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    setTimeout(() => window.URL.revokeObjectURL(url), 10000);
  },
};

export const NotificationApi = {
  getMyNotifications: () => 
    apiRequest('/api/v1/notifications/my'),

  markAsRead: (notificationId) => 
    apiRequest(`/api/v1/notifications/${encodeURIComponent(notificationId)}/read`, {
      method: 'PATCH',
    }),
};
