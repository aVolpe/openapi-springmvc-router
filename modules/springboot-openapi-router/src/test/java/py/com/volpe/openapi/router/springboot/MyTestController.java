package py.com.volpe.openapi.router.springboot;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Arturo Volpe
 * @since 2023-02-06
 */
@RestController
public class MyTestController {

    public record Pet(String name, int id, String type) {
    }

    private static final List<Pet> pets = new ArrayList<>();

    public List<Pet> listPets() {
        return pets;
    }

    public Pet createPet(@RequestBody @Validated Pet newPet) {
        Pet toAdd = new Pet(
                newPet.name,
                pets.stream().mapToInt(p -> p.id).max().orElse(1),
                newPet.type
        );
        pets.add(toAdd);
        return toAdd;
    }
}
