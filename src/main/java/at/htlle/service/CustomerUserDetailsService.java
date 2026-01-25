package at.htlle.service;

import at.htlle.entity.Customer;
import at.htlle.repository.CustomerRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomerUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        boolean enabled = customer.getStatus() == Customer.Status.ACTIVE;
        return new User(
                customer.getUsername(),
                customer.getPassword(),
                enabled,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority("ROLE_" + customer.getRole().name()))
        );
    }
}
