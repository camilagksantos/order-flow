package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.NIF;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CustomerEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {AddressPersistenceMapper.class})
public interface CustomerPersistenceMapper {

    @Mapping(target = "email", source = "email")
    @Mapping(target = "nif", source = "nif")
    @Mapping(target = "userId", source = "user.id")
    Customer toDomain(CustomerEntity entity);

    @Mapping(target = "email", source = "email")
    @Mapping(target = "nif", source = "nif")
    @Mapping(target = "user", source = "userId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CustomerEntity toEntity(Customer domain);

    @AfterMapping
    default void linkAddressesToCustomer(@MappingTarget CustomerEntity entity) {
        if (entity.getAddresses() != null) {
            entity.getAddresses().forEach(address -> address.setCustomer(entity));
        }
    }

    default UserEntity toUserEntity(Long userId) {
        if (userId == null) return null;
        UserEntity user = new UserEntity();
        user.setId(userId);
        return user;
    }

    default Long fromUserEntity(UserEntity user) {
        return user != null ? user.getId() : null;
    }

    default Email toEmail(String value) {
        return new Email(value);
    }

    default String fromEmail(Email email) {
        return email.value();
    }

    default NIF toNIF(String value) {
        return new NIF(value);
    }

    default String fromNIF(NIF nif) {
        return nif.value();
    }
}