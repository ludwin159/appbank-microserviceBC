package com.bank.appbank.service.impl;

import com.bank.appbank.exceptions.ClientAlreadyExist;
import com.bank.appbank.exceptions.InconsistentClientException;
import com.bank.appbank.exceptions.ResourceNotFoundException;
import com.bank.appbank.factory.RepositoryFactory;
import com.bank.appbank.model.Client;
import com.bank.appbank.repository.ClientRepository;
import com.bank.appbank.service.ClientService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import static com.bank.appbank.model.Client.TypeClient.*;

@Service
public class ClientServiceImpl extends ServiceGenImp<Client, String> implements ClientService {

    @Override
    protected Class<Client> getEntityClass() {
        return Client.class;
    }

    public ClientServiceImpl(RepositoryFactory repositoryFactory) {
        super(repositoryFactory);
    }

    @Override
    public Mono<Client> updateClient(String id, Client client) {
        return getRepository().findById(id)
                .flatMap(clientFound -> {
                    clientFound.setFullName(client.getFullName());
                    clientFound.setEmail(client.getEmail());
                    clientFound.setTaxId(client.getTaxId());
                    clientFound.setBusinessName(client.getBusinessName());
                    clientFound.setAddress(client.getAddress());
                    clientFound.setPhone(client.getPhone());
                    return getRepository().save(clientFound);
                })
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Client not found")));
    }

    @Override
    public Mono<Client> create(Client clientNew) {
        return isClientValid(clientNew)
                .flatMap(value -> {
                    if (value.equals("NOT_EXISTS"))
                        return getRepository().save(clientNew);
                    return Mono.error(new ClientAlreadyExist("The client already exist in the data base"));
                });
    }

    private boolean isPersonalTypeClient(Client client) {
        return client.getTypeClient() == PERSONAL_CLIENT || client.getTypeClient() == PERSONAL_VIP_CLIENT;
    }

    private Mono<String> isClientValid(Client client) {
        if (isPersonalTypeClient(client) && !isValidPersonalClient(client)) {
            return Mono.error(new InconsistentClientException(
                    "The personal client must have identification and full name, but not a business name."));
        }

        if (!isPersonalTypeClient(client) && !isValidBusinessClient(client)) {
            return Mono.error(new InconsistentClientException(
                    "The business client must have tax id and business name, but not a full name or identity."));
        }

        return checkIfClientExists(client);
    }

    private boolean isValidPersonalClient(Client client) {
        return !client.getIdentity().isEmpty() &&
                !client.getFullName().isEmpty() &&
                client.getTaxId().isEmpty() &&
                client.getBusinessName().isEmpty();
    }

    private boolean isValidBusinessClient(Client client) {
        return client.getIdentity().isEmpty() &&
                client.getFullName().isEmpty() &&
                !client.getTaxId().isEmpty() &&
                !client.getBusinessName().isEmpty();
    }

    private Mono<String> checkIfClientExists(Client client) {
        ClientRepository repository = (ClientRepository) getRepository();

        Mono<Client> existingClientMono = isPersonalTypeClient(client)
                ? repository.findByIdentity(client.getIdentity())
                : repository.findByTaxId(client.getTaxId());

        return existingClientMono
                .map(existing -> "EXIST")
                .defaultIfEmpty("NOT_EXISTS");
    }

    @Override
    public Flux<Client> findAllClientsById(List<String> ids) {
        return getRepository().findAllById(ids);
    }

}
