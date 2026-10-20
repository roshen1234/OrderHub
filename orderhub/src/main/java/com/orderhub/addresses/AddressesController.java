package com.orderhub.addresses;

import com.orderhub.addresses.DTO.AddressesRequest;
import com.orderhub.addresses.DTO.AddressesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressesController {

    public final AddressesService addressesService;


    @PostMapping
    public ResponseEntity<AddressesResponse> add(@Valid @RequestBody AddressesRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressesService.add(authentication.getName(), request));
    }

    @GetMapping
    public List<AddressesResponse> list(Authentication authentication) {
        return addressesService.list(authentication.getName());
    }

    @PutMapping("/{id}")
    public AddressesResponse update(@PathVariable Long id, @Valid @RequestBody AddressesRequest request,
                                  Authentication authentication) {
        return addressesService.update(authentication.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        addressesService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

}
