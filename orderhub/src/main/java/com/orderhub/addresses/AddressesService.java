package com.orderhub.addresses;

import com.orderhub.addresses.DTO.AddressesRequest;
import com.orderhub.addresses.DTO.AddressesResponse;
import com.orderhub.common.exception.BadRequestException;
import com.orderhub.common.exception.ResourceNotFoundException;
import com.orderhub.user.User;
import com.orderhub.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressesService {

    private static final int MAX_ADDRESSES = 10;

    private final UserRepository userRepository;
    private final AddressesRepository addressRepository;

    @Transactional
    public AddressesResponse add(String email, AddressesRequest r) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        long count = addressRepository.countByUserEmail(email);
        if (count >= MAX_ADDRESSES) {
            throw new BadRequestException("You can save at most " + MAX_ADDRESSES + " addresses");
        }

        Addresses a = new Addresses();
        a.setUser(user);
        copyFields(a, r);

        boolean makeDefault = count == 0 || Boolean.TRUE.equals(r.makeDefault());   // the first address is the default
        if (makeDefault) {
            addressRepository.clearDefault(user.getId());
        }
        a.setDefaultAddress(makeDefault);
        return AddressesResponse.from(addressRepository.save(a));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<AddressesResponse> list(String email) {
        return addressRepository.findByUserEmailOrderByDefaultAddressDescCreatedAtDesc(email)
                .stream().map(AddressesResponse::from).toList();
    }

    @Transactional
    public AddressesResponse update(String email, Long id, AddressesRequest r) {
        Addresses a = addressRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Address", id));   // not yours: 404

        copyFields(a, r);
        if (Boolean.TRUE.equals(r.makeDefault()) && !a.isDefaultAddress()) {
            addressRepository.clearDefault(a.getUser().getId());
            a.setDefaultAddress(true);
        }
        return AddressesResponse.from(a);
    }

    @Transactional
    public void delete(String email, Long id) {
        Addresses a = addressRepository.findByIdAndUserEmail(id, email)
                .orElseThrow(() -> new ResourceNotFoundException("Address", id));
        addressRepository.delete(a);
    }

    private void copyFields(Addresses a, AddressesRequest r) {
        a.setLabel(r.label() == null ? null : r.label().trim());
        a.setRecipientName(r.recipientName().trim());
        a.setPhone(r.phone().trim());
        a.setLine1(r.line1().trim());
        a.setLine2(r.line2() == null ? null : r.line2().trim());
        a.setCity(r.city().trim());
        a.setState(r.state().trim());
        a.setPostalCode(r.postalCode().trim());
        a.setCountry(r.country().trim().toUpperCase());
    }
}