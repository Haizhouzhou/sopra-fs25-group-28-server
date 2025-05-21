package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

  DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

  @Mapping(source = "name", target = "name")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "password", target = "password")
  @Mapping(source = "token", target = "token")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "wincounter", target = "wincounter")

  User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "name", target = "name")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "creation_date", target = "creation_date")
  @Mapping(source = "birthday", target = "birthday")
  @Mapping(source = "avatar", target = "avatar")
  @Mapping(source = "wincounter", target = "wincounter")
  UserGetDTO convertEntityToUserGetDTO(User user);

  @Mapping(source = "token", target = "token")
  LoginTokenGetDTO convertEntityToLoginTokenGetDTO(User user);

  @Mapping(source = "username", target = "username")
  @Mapping(source = "password", target = "password")
  @Mapping(source = "token", target = "token")
  User convertLoginCredentialPostDTOtoUser(LoginCredentialPostDTO loginCredential);

  @Mapping(source = "name", target = "name")
  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "wincounter", target = "wincounter")
  @Mapping(source = "avatar", target = "avatar")
  UserListGetDTO convertUserToUserListGetDTO(User user);


  

  default User convertUserPostDTOtoEntityWithDefaults(UserPostDTO userPostDTO) {
    User user = convertUserPostDTOtoEntity(userPostDTO);
    user.setWincounter(0); // set default win counter
    return user;
}


}
