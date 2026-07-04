import React, { useEffect, useState } from 'react';
import { addressApi, type AddressResponse, type AddressRequest } from '../api/addressApi';
import { Plus, Edit2, Trash2, CheckCircle2, AlertCircle, Home, MapPin, X } from 'lucide-react';

export const AddressBook: React.FC = () => {
  const [addresses, setAddresses] = useState<AddressResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form State
  const [showForm, setShowForm] = useState(false);
  const [editingAddress, setEditingAddress] = useState<AddressResponse | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  
  // Input fields
  const [label, setLabel] = useState('');
  const [recipientName, setRecipientName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [addressLine1, setAddressLine1] = useState('');
  const [addressLine2, setAddressLine2] = useState('');
  const [city, setCity] = useState('');
  const [region, setRegion] = useState('');
  const [postalCode, setPostalCode] = useState('');
  const [country, setCountry] = useState('');

  const [formValidationError, setFormValidationError] = useState<string | null>(null);
  const [formApiError, setFormApiError] = useState<string | null>(null);

  const fetchAddresses = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await addressApi.getAddresses();
      setAddresses(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load address book.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchAddresses();
  }, []);

  const openCreateForm = () => {
    setEditingAddress(null);
    setLabel('');
    setRecipientName('');
    setPhoneNumber('');
    setAddressLine1('');
    setAddressLine2('');
    setCity('');
    setRegion('');
    setPostalCode('');
    setCountry('');
    setFormValidationError(null);
    setFormApiError(null);
    setShowForm(true);
  };

  const openEditForm = (address: AddressResponse) => {
    setEditingAddress(address);
    setLabel(address.label || '');
    setRecipientName(address.recipientName);
    setPhoneNumber(address.phoneNumber);
    setAddressLine1(address.addressLine1);
    setAddressLine2(address.addressLine2 || '');
    setCity(address.city);
    setRegion(address.region || '');
    setPostalCode(address.postalCode || '');
    setCountry(address.country);
    setFormValidationError(null);
    setFormApiError(null);
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditingAddress(null);
  };

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormValidationError(null);
    setFormApiError(null);

    // Client-side validations
    if (
      !recipientName.trim() ||
      !phoneNumber.trim() ||
      !addressLine1.trim() ||
      !city.trim() ||
      !country.trim()
    ) {
      setFormValidationError('Please fill in all required fields (Recipient, Phone, Address Line 1, City, Country).');
      return;
    }

    if (label.length > 100) {
      setFormValidationError('Label cannot exceed 100 characters.');
      return;
    }
    if (recipientName.length > 200) {
      setFormValidationError('Recipient name cannot exceed 200 characters.');
      return;
    }
    if (phoneNumber.length > 32) {
      setFormValidationError('Phone number cannot exceed 32 characters.');
      return;
    }
    if (addressLine1.length > 255 || addressLine2.length > 255) {
      setFormValidationError('Address lines cannot exceed 255 characters.');
      return;
    }
    if (city.length > 120 || region.length > 120 || country.length > 120) {
      setFormValidationError('City, Region, and Country cannot exceed 120 characters.');
      return;
    }
    if (postalCode.length > 32) {
      setFormValidationError('Postal code cannot exceed 32 characters.');
      return;
    }

    const payload: AddressRequest = {
      label: label.trim() || undefined,
      recipientName: recipientName.trim(),
      phoneNumber: phoneNumber.trim(),
      addressLine1: addressLine1.trim(),
      addressLine2: addressLine2.trim() || undefined,
      city: city.trim(),
      region: region.trim() || undefined,
      postalCode: postalCode.trim() || undefined,
      country: country.trim()
    };

    setIsSaving(true);
    try {
      if (editingAddress) {
        await addressApi.updateAddress(editingAddress.id, payload);
      } else {
        await addressApi.createAddress(payload);
      }
      closeForm();
      await fetchAddresses();
    } catch (err: any) {
      setFormApiError(err.message || 'Failed to save address.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteAddress = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this address?')) return;
    try {
      await addressApi.deleteAddress(id);
      await fetchAddresses();
    } catch (err: any) {
      setError(err.message || 'Failed to delete address.');
    }
  };

  const handleSetDefault = async (id: number) => {
    try {
      await addressApi.setDefaultAddress(id);
      await fetchAddresses();
    } catch (err: any) {
      setError(err.message || 'Failed to set default address.');
    }
  };

  if (isLoading && addresses.length === 0) {
    return (
      <div className="flex-center" style={{ height: '400px' }}>
        <div style={{
          border: '4px solid var(--border)',
          borderTopColor: 'var(--accent)',
          borderRadius: 'var(--radius-full)',
          width: '40px',
          height: '40px',
          animation: 'spin 1s linear infinite'
        }} />
        <style>{`
          @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
          }
        `}</style>
      </div>
    );
  }

  return (
    <div className="container animate-fade-in" style={{ padding: '40px 20px 80px 20px' }}>
      <div className="flex-between" style={{ marginBottom: '30px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h1 className="title-medium" style={{ marginBottom: '8px' }}>Address Book</h1>
          <p style={{ color: 'var(--text-secondary)' }}>Manage your saved shipping locations.</p>
        </div>
        <button onClick={openCreateForm} className="btn btn-primary flex-center">
          <Plus size={18} />
          <span>Add New Address</span>
        </button>
      </div>

      {error && (
        <div className="alert alert-error" style={{ marginBottom: '30px' }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Address cards list */}
      {addresses.length === 0 ? (
        <div style={{
          textAlign: 'center',
          padding: '60px 20px',
          backgroundColor: 'var(--bg-card)',
          borderRadius: 'var(--radius-lg)',
          border: '1px solid var(--border)'
        }}>
          <MapPin size={48} style={{ color: 'var(--text-muted)', marginBottom: '16px' }} />
          <h3 className="title-small" style={{ marginBottom: '8px' }}>No saved addresses</h3>
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px' }}>
            Please add an address to make your checkout experience seamless.
          </p>
          <button onClick={openCreateForm} className="btn btn-primary flex-center" style={{ margin: '0 auto' }}>
            <Plus size={16} />
            <span>Create First Address</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-3">
          {addresses.map((address) => (
            <div
              key={address.id}
              className={`address-card-premium ${address.isDefault ? 'is-selected' : ''}`}
              style={{
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                gap: '16px'
              }}
            >
              <div>
                <div className="flex-between" style={{ marginBottom: '12px' }}>
                  <span style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '4px',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    textTransform: 'uppercase',
                    color: 'var(--accent)',
                    backgroundColor: 'var(--bg-secondary)',
                    border: '1px solid var(--border)',
                    padding: '2px 8px',
                    borderRadius: 'var(--radius-sm)'
                  }}>
                    <Home size={12} />
                    <span>{address.label || 'Saved Location'}</span>
                  </span>

                  {address.isDefault && (
                    <span className="flex-center" style={{
                      gap: '4px',
                      color: 'var(--success)',
                      fontSize: '0.75rem',
                      fontWeight: 600
                    }}>
                      <CheckCircle2 size={14} />
                      <span>Default</span>
                    </span>
                  )}
                </div>

                <h3 style={{ fontSize: '1.05rem', fontWeight: 600, marginBottom: '4px' }}>
                  {address.recipientName}
                </h3>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '4px' }}>
                  {address.phoneNumber}
                </p>
                <p style={{ fontSize: '0.875rem', color: 'var(--text-primary)', lineHeight: 1.5 }}>
                  {address.addressLine1}
                  {address.addressLine2 && `, ${address.addressLine2}`}
                  <br />
                  {address.city}
                  {address.region && `, ${address.region}`}
                  {address.postalCode && ` (${address.postalCode})`}
                  <br />
                  {address.country}
                </p>
              </div>

              {/* Action Buttons footer */}
              <div style={{ display: 'flex', gap: '8px', borderTop: '1px solid var(--border)', paddingTop: '16px' }}>
                {!address.isDefault && (
                  <button
                    onClick={() => handleSetDefault(address.id)}
                    className="btn btn-secondary"
                    style={{ flexGrow: 1, padding: '8px 12px', fontSize: '0.75rem' }}
                  >
                    Set Default
                  </button>
                )}
                <button
                  onClick={() => openEditForm(address)}
                  className="btn btn-secondary flex-center"
                  style={{ padding: '8px 12px' }}
                  title="Edit Address"
                >
                  <Edit2 size={14} />
                </button>
                <button
                  onClick={() => handleDeleteAddress(address.id)}
                  className="btn btn-secondary flex-center"
                  style={{ padding: '8px 12px' }}
                  title="Delete Address"
                  onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--error)')}
                  onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-primary)')}
                >
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Form Dialog Panel */}
      {showForm && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100%',
          height: '100%',
          backgroundColor: 'rgba(0, 0, 0, 0.4)',
          backdropFilter: 'blur(4px)',
          zIndex: 1000,
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          padding: '20px'
        }}>
          <div className="card" style={{
            width: '100%',
            maxWidth: '600px',
            maxHeight: '90vh',
            overflowY: 'auto',
            padding: '30px',
            backgroundColor: 'var(--bg-card)',
            boxShadow: 'var(--shadow-xl)',
            position: 'relative'
          }}>
            {/* Close Button */}
            <button onClick={closeForm} style={{
              position: 'absolute',
              top: '20px',
              right: '20px',
              border: 'none',
              background: 'none',
              cursor: 'pointer',
              color: 'var(--text-muted)'
            }} onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--text-primary)')}
               onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}>
              <X size={20} />
            </button>

            <h2 className="title-small" style={{ marginBottom: '24px', fontWeight: 700 }}>
              {editingAddress ? 'Modify Address' : 'Add New Address'}
            </h2>

            {/* Error alerts */}
            {formValidationError && (
              <div className="alert alert-error" style={{ fontSize: '0.85rem', padding: '12px', marginBottom: '20px' }}>
                <AlertCircle size={16} />
                <span>{formValidationError}</span>
              </div>
            )}
            {formApiError && (
              <div className="alert alert-error" style={{ fontSize: '0.85rem', padding: '12px', marginBottom: '20px' }}>
                <AlertCircle size={16} />
                <span>{formApiError}</span>
              </div>
            )}

            {/* Form Fields */}
            <form onSubmit={handleFormSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Row 1: Label & Recipient */}
              <div className="grid grid-2" style={{ gap: '16px' }}>
                <div className="form-group-premium">
                  <label className="form-label-premium">Label (e.g. Home, Work)</label>
                  <input
                    type="text"
                    placeholder="Home"
                    className="input-field"
                    value={label}
                    onChange={(e) => setLabel(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
                <div className="form-group-premium">
                  <label className="form-label-premium">Recipient Name *</label>
                  <input
                    type="text"
                    placeholder="John Doe"
                    className="input-field"
                    value={recipientName}
                    onChange={(e) => setRecipientName(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
              </div>

              {/* Row 2: Phone & Address Line 1 */}
              <div className="grid grid-2" style={{ gap: '16px' }}>
                <div className="form-group-premium">
                  <label className="form-label-premium">Phone Number *</label>
                  <input
                    type="text"
                    placeholder="+1 555-0199"
                    className="input-field"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
                <div className="form-group-premium">
                  <label className="form-label-premium">Address Line 1 *</label>
                  <input
                    type="text"
                    placeholder="123 Main St"
                    className="input-field"
                    value={addressLine1}
                    onChange={(e) => setAddressLine1(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
              </div>

              {/* Row 3: Address Line 2 & City */}
              <div className="grid grid-2" style={{ gap: '16px' }}>
                <div className="form-group-premium">
                  <label className="form-label-premium">Address Line 2 (Apt, Suite)</label>
                  <input
                    type="text"
                    placeholder="Apt 4B"
                    className="input-field"
                    value={addressLine2}
                    onChange={(e) => setAddressLine2(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
                <div className="form-group-premium">
                  <label className="form-label-premium">City *</label>
                  <input
                    type="text"
                    placeholder="New York"
                    className="input-field"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
              </div>

              {/* Row 4: Region & Postal Code & Country */}
              <div className="grid grid-3" style={{ gap: '16px' }}>
                <div className="form-group-premium">
                  <label className="form-label-premium">Region/State</label>
                  <input
                    type="text"
                    placeholder="NY"
                    className="input-field"
                    value={region}
                    onChange={(e) => setRegion(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
                <div className="form-group-premium">
                  <label className="form-label-premium">Postal Code</label>
                  <input
                    type="text"
                    placeholder="10001"
                    className="input-field"
                    value={postalCode}
                    onChange={(e) => setPostalCode(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
                <div className="form-group-premium">
                  <label className="form-label-premium">Country *</label>
                  <input
                    type="text"
                    placeholder="United States"
                    className="input-field"
                    value={country}
                    onChange={(e) => setCountry(e.target.value)}
                    disabled={isSaving}
                  />
                </div>
              </div>

              {/* Actions Row */}
              <div style={{ display: 'flex', gap: '16px', marginTop: '20px', justifyContent: 'flex-end' }}>
                <button type="button" onClick={closeForm} className="btn btn-secondary" style={{ padding: '12px 24px' }} disabled={isSaving}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" style={{ padding: '12px 24px' }} disabled={isSaving}>
                  {isSaving ? 'Saving...' : 'Save Address'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
