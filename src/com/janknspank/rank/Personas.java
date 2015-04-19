package com.janknspank.rank;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.protobuf.TextFormat;
import com.janknspank.bizness.GuidFactory;
import com.janknspank.proto.RankProto.Persona;
import com.janknspank.proto.UserProto.AddressBookContact;
import com.janknspank.proto.UserProto.LinkedInContact;
import com.janknspank.proto.UserProto.User;

/**
 * Class for converting us over to neural network training personas.
 */
public class Personas {
  /**
   * Map from email address to Persona object.
   */
  private static Map<String, Persona> PERSONA_MAP = null;

  /**
   * Builds a Map of Persona object definitions, keyed by persona email address, from
   * the .persona files located in /personas/.
   */
  public static synchronized Map<String, Persona> getPersonaMap() {
    if (PERSONA_MAP == null) {
      ImmutableMap.Builder<String, Persona> personaListBuilder = ImmutableMap.builder();
      for (File personaFile : new File("personas/").listFiles()) {
        if (!personaFile.getName().endsWith(".persona")) {
          continue;
        }
        Persona.Builder personaBuilder = Persona.newBuilder();
        Reader reader = null;
        try {
          reader = new FileReader(personaFile);
          try {
            TextFormat.merge(reader, personaBuilder);
          } catch (TextFormat.ParseException e) {
            throw new Error(
                "Error in persona " + personaFile.getAbsolutePath() + ": " + e.getMessage(), e);
          }
          personaListBuilder.put(personaBuilder.getEmail(), personaBuilder.build());
        } catch (IOException e) {
          throw new Error(e);
        } finally {
          IOUtils.closeQuietly(reader);
        }
      }
      PERSONA_MAP = personaListBuilder.build();
    }
    return PERSONA_MAP;
  }

  public static Persona getByEmail(String email) {
    return getPersonaMap().get(email);
  }

  /**
   * Creates a stub User object from the interest-oriented fields in the passed
   * Persona object.
   *
   * NOTE(jonemerson): The persona should have everything necessary for properly
   * ranking articles against a User, and the Scorers should be able to deal
   * with these stub User objects without error or special casing.  If something
   * ends up to be missing, we should handle it by improving personas!
   */
  public static User convertToUser(Persona persona) {
    // First name.
    String firstName =
        persona.getName().substring(0, persona.getName().indexOf(" ") + 1).trim();

    // LinkedIn contacts.
    List<LinkedInContact> linkedInContacts = Lists.newArrayList();
    for (String linkedInContactName : persona.getLinkedInContactNameList()) {
      linkedInContacts.add(
          LinkedInContact.newBuilder().setName(linkedInContactName).build());
    }

    // Address book contacts.
    List<AddressBookContact> addressBookContacts = Lists.newArrayList();
    for (String addressBookContactName : persona.getLinkedInContactNameList()) {
      addressBookContacts.add(
          AddressBookContact.newBuilder().setName(addressBookContactName).build());
    }

    // OK let's go.
    return User.newBuilder()
        .setId(GuidFactory.generate())
        .setFirstName(firstName)
        .setLastName(persona.getName().substring(firstName.length()).trim())
        .addAllInterest(persona.getInterestList())
        .addAllLinkedInContact(linkedInContacts)
        .addAllAddressBookContact(addressBookContacts)
        .setEmail(persona.getEmail())
        .build();
  }
}
