package com.lantanagroup.link.validation;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.*;
import com.lantanagroup.link.Constants;
import com.lantanagroup.link.FhirContextProvider;
import org.hl7.fhir.common.hapi.validation.support.*;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.utilities.i18n.I18nConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {
  protected static final Logger logger = LoggerFactory.getLogger(Validator.class);

  private volatile FhirValidator validator;

  private static OperationOutcome.IssueSeverity getIssueSeverity(ResultSeverityEnum severity) {
    switch (severity) {
      case ERROR:
        return OperationOutcome.IssueSeverity.ERROR;
      case WARNING:
        return OperationOutcome.IssueSeverity.WARNING;
      case INFORMATION:
        return OperationOutcome.IssueSeverity.INFORMATION;
      case FATAL:
        return OperationOutcome.IssueSeverity.FATAL;
      default:
        throw new RuntimeException("Unexpected severity " + severity);
    }
  }

  private static OperationOutcome.IssueType getIssueCode(String messageId) {
    if (messageId == null) {
      return OperationOutcome.IssueType.NULL;
    } else if (messageId.startsWith("Rule ")) {
      return OperationOutcome.IssueType.INVARIANT;
    }

    switch (messageId) {
      case I18nConstants.DUPLICATE_ID:
      case I18nConstants.LANGUAGE_XHTML_LANG_DIFFERENT1:
      case I18nConstants.LANGUAGE_XHTML_LANG_DIFFERENT2:
      case I18nConstants.LANGUAGE_XHTML_LANG_MISSING1:
      case I18nConstants.LANGUAGE_XHTML_LANG_MISSING2:
      case I18nConstants.LANGUAGE_XHTML_LANG_MISSING3:
      case I18nConstants.MEASURE_MR_GRP_DUPL_CODE:
      case I18nConstants.MEASURE_MR_GRP_MISSING_BY_CODE:
      case I18nConstants.MEASURE_MR_GRP_NO_CODE:
      case I18nConstants.MEASURE_MR_GRP_NO_USABLE_CODE:
      case I18nConstants.MEASURE_MR_GRP_NO_WRONG_CODE:
      case I18nConstants.MEASURE_MR_GRP_POP_COUNT_MISMATCH:
      case I18nConstants.MEASURE_MR_GRP_POP_DUPL_CODE:
      case I18nConstants.MEASURE_MR_GRP_POP_NO_CODE:
      case I18nConstants.MEASURE_MR_GRP_POP_NO_COUNT:
      case I18nConstants.MEASURE_MR_GRP_POP_NO_SUBJECTS:
      case I18nConstants.MEASURE_MR_GRP_POP_UNK_CODE:
      case I18nConstants.MEASURE_MR_GRP_UNK_CODE:
      case I18nConstants.MEASURE_MR_GRPST_POP_UNK_CODE:
      case I18nConstants.MEASURE_MR_M_SCORING_UNK:
      case I18nConstants.MEASURE_MR_SCORE_PROHIBITED_MS:
      case I18nConstants.MEASURE_MR_SCORE_PROHIBITED_RT:
      case I18nConstants.MEASURE_MR_SCORE_UNIT_PROHIBITED:
      case I18nConstants.META_RES_SECURITY_DUPLICATE:
      case I18nConstants.MULTIPLE_LOGICAL_MODELS:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_NO_ANNOTATIONS:
      case I18nConstants.VALIDATION_VAL_PROFILE_DEPENDS_NOT_RESOLVED:
      case I18nConstants.VALIDATION_VAL_STATUS_INCONSISTENT:
      case I18nConstants.VALIDATION_VAL_STATUS_INCONSISTENT_HINT:
        return OperationOutcome.IssueType.BUSINESSRULE;
      case I18nConstants.MEASURE_MR_SCORE_FIXED:
      case I18nConstants.TERMINOLOGY_PASSTHROUGH_TX_MESSAGE:
      case I18nConstants.TERMINOLOGY_TX_BINDING_CANTCHECK:
      case I18nConstants.TERMINOLOGY_TX_BINDING_MISSING:
      case I18nConstants.TERMINOLOGY_TX_BINDING_MISSING2:
      case I18nConstants.TERMINOLOGY_TX_BINDING_NOSERVER:
      case I18nConstants.TERMINOLOGY_TX_BINDING_NOSOURCE:
      case I18nConstants.TERMINOLOGY_TX_BINDING_NOSOURCE2:
      case I18nConstants.TERMINOLOGY_TX_CODE_NOTVALID:
      case I18nConstants.TERMINOLOGY_TX_CODE_UNKNOWN:
      case I18nConstants.TERMINOLOGY_TX_CODE_VALUESET:
      case I18nConstants.TERMINOLOGY_TX_CODE_VALUESET_EXT:
      case I18nConstants.Terminology_TX_Code_ValueSet_MISSING:
      case I18nConstants.TERMINOLOGY_TX_CODE_VALUESETMAX:
      case I18nConstants.TERMINOLOGY_TX_CONFIRM_1_CC:
      case I18nConstants.TERMINOLOGY_TX_CONFIRM_2_CC:
      case I18nConstants.TERMINOLOGY_TX_CONFIRM_3_CC:
      case I18nConstants.TERMINOLOGY_TX_CONFIRM_4a:
      case I18nConstants.TERMINOLOGY_TX_CONFIRM_5:
      case I18nConstants.TERMINOLOGY_TX_CONFIRM_6:
      case I18nConstants.TERMINOLOGY_TX_DISPLAY_WRONG:
      case I18nConstants.TERMINOLOGY_TX_ERROR_CODEABLECONCEPT:
      case I18nConstants.TERMINOLOGY_TX_ERROR_CODEABLECONCEPT_MAX:
      case I18nConstants.TERMINOLOGY_TX_ERROR_CODING1:
      case I18nConstants.TERMINOLOGY_TX_ERROR_CODING2:
      case I18nConstants.TERMINOLOGY_TX_NOSVC_BOUND_EXT:
      case I18nConstants.TERMINOLOGY_TX_NOSVC_BOUND_REQ:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_1_CC:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_10:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_11:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_12:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_13:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_14:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_15:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_15A:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_16:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_17:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_18:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_2_CC:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_3_CC:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_4:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_5:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_6:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_7:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_8:
      case I18nConstants.TERMINOLOGY_TX_NOVALID_9:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_HTTPS:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_INVALID:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_NO_CODE:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_RELATIVE:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_UNKNOWN:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_VALUESET:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_VALUESET2:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_WRONG_BUILD:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_WRONG_HTML:
      case I18nConstants.TERMINOLOGY_TX_VALUESET_NOTFOUND:
      case I18nConstants.TERMINOLOGY_TX_VALUESET_NOTFOUND_CS:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_IDENTIFIER_IETF_SYSTEM_VALUE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_IDENTIFIER_SYSTEM:
      case I18nConstants.UNICODE_BIDI_CONTROLS_CHARS_DISALLOWED:
        return OperationOutcome.IssueType.CODEINVALID;
      case I18nConstants.INTERNAL_INT_BAD_TYPE:
        return OperationOutcome.IssueType.EXCEPTION;
      case I18nConstants.BUNDLE_BUNDLE_ENTRY_TYPE:
      case I18nConstants.BUNDLE_BUNDLE_ENTRY_TYPE2:
      case I18nConstants.BUNDLE_BUNDLE_ENTRY_TYPE3:
      case I18nConstants.DEFINED_IN_THE_PROFILE:
      case I18nConstants.MUSTSUPPORT_VAL_MUSTSUPPORT:
      case I18nConstants.TERMINOLOGY_TX_HINT:
      case I18nConstants.TERMINOLOGY_TX_WARNING:
      case I18nConstants.THIS_ELEMENT_DOES_NOT_MATCH_ANY_KNOWN_SLICE_:
      case I18nConstants.VALIDATION_VAL_PROFILE_SIGNPOST:
      case I18nConstants.VALIDATION_VAL_PROFILE_SIGNPOST_DEP:
      case I18nConstants.VALIDATION_VAL_PROFILE_SIGNPOST_GLOBAL:
      case I18nConstants.VALIDATION_VAL_PROFILE_SIGNPOST_META:
        return OperationOutcome.IssueType.INFORMATIONAL;
      case I18nConstants.ALL_OBSERVATIONS_SHOULD_HAVE_A_PERFORMER:
      case I18nConstants.ALL_OBSERVATIONS_SHOULD_HAVE_A_SUBJECT:
      case I18nConstants.ALL_OBSERVATIONS_SHOULD_HAVE_AN_EFFECTIVEDATETIME_OR_AN_EFFECTIVEPERIOD:
      case I18nConstants.BUNDLE_BUNDLE_ENTRY_MULTIPLE_PROFILES:
      case I18nConstants.BUNDLE_BUNDLE_ENTRY_NOPROFILE_EXPL:
      case I18nConstants.BUNDLE_BUNDLE_ENTRY_NOPROFILE_TYPE:
      case I18nConstants.CAPABALITYSTATEMENT_CS_SP_WRONGTYPE:
      case I18nConstants.ELEMENT_CANNOT_BE_NULL:
      case I18nConstants.EXT_VER_URL_IGNORE:
      case I18nConstants.EXT_VER_URL_MISLEADING:
      case I18nConstants.EXT_VER_URL_NO_MATCH:
      case I18nConstants.EXT_VER_URL_NOT_ALLOWED:
      case I18nConstants.EXT_VER_URL_REVERSION:
      case I18nConstants.EXTENSION_EXT_SUBEXTENSION_INVALID:
      case I18nConstants.EXTENSION_EXT_URL_ABSOLUTE:
      case I18nConstants.EXTENSION_EXT_URL_NOTFOUND:
      case I18nConstants.MEASURE_M_CRITERIA_CQL_ELM_NOT_VALID:
      case I18nConstants.MEASURE_M_CRITERIA_CQL_ERROR:
      case I18nConstants.MEASURE_M_CRITERIA_CQL_LIB_DUPL:
      case I18nConstants.MEASURE_M_CRITERIA_CQL_LIB_NOT_FOUND:
      case I18nConstants.MEASURE_M_CRITERIA_CQL_NO_ELM:
      case I18nConstants.MEASURE_M_CRITERIA_CQL_NO_LIB:
      case I18nConstants.MEASURE_M_CRITERIA_CQL_NOT_FOUND:
      case I18nConstants.MEASURE_M_CRITERIA_CQL_ONLY_ONE_LIB:
      case I18nConstants.PRIMITIVE_MUSTHAVEVALUE_MESSAGE:
      case I18nConstants.PRIMITIVE_VALUE_ALTERNATIVES_MESSAGE:
      case I18nConstants.REFERENCE_REF_QUERY_INVALID:
      case I18nConstants.RESOURCE_RES_ID_MALFORMED_CHARS:
      case I18nConstants.RESOURCE_RES_ID_MALFORMED_LENGTH:
      case I18nConstants.RESOURCE_RES_ID_MISSING:
      case I18nConstants.RESOURCE_RES_ID_PROHIBITED:
      case I18nConstants.SD_TYPE_MISSING:
      case I18nConstants.SD_TYPE_NOT_DERIVED:
      case I18nConstants.SD_TYPE_NOT_LOCAL:
      case I18nConstants.SD_TYPE_NOT_LOGICAL:
      case I18nConstants.SD_TYPE_NOT_MATCH_NS:
      case I18nConstants.SECURITY_STRING_CONTENT_ERROR:
      case I18nConstants.SECURITY_STRING_CONTENT_WARNING:
      case I18nConstants.STATUS_CODE_HINT:
      case I18nConstants.STATUS_CODE_HINT_CODE:
      case I18nConstants.STATUS_CODE_WARNING:
      case I18nConstants.STATUS_CODE_WARNING_CODE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_CANONICAL_ABSOLUTE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_CANONICAL_CONTAINED:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_BASE64_NO_WS_ERROR:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_BASE64_NO_WS_WARNING:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_BASE64_VALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_BOOLEAN_VALUE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_CANONICAL_RESOLVE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_CANONICAL_RESOLVE_NC:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_CANONICAL_TYPE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_CODE_WS:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_DATE_VALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_DATETIME_REASONABLE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_DATETIME_REGEX:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_DATETIME_TZ:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_DATETIME_VALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_DECIMAL_GT:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_DECIMAL_LT:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_DECIMAL_VALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_ID_VALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_INSTANT_VALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_INTEGER_GT:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_INTEGER_LT:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_INTEGER_LT0:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_INTEGER_LT1:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_INTEGER_VALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_INTEGER64_VALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_MARKDOWN_HTML:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_OID_START:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_OID_VALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_PRIMITIVE_LENGTH:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_PRIMITIVE_NOTEMPTY:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_PRIMITIVE_REGEX:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_PRIMITIVE_REGEX_EXCEPTION:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_PRIMITIVE_REGEX_TYPE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_PRIMITIVE_VALUEEXT:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_PRIMITIVE_WS:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_CODE_MISMATCH:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_MIN_NO_CODE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_MIN_NO_CONVERT:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_MIN_NO_SYSTEM:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_MIN_NO_VALUE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_NO_QTY:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_NO_UCUM_SVC:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_SYSTEM_MISMATCH:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_VALUE_NO_CODE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_VALUE_NO_SYSTEM:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_VALUE_NO_VALUE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_VALUE_WRONG:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MAX_VALUE_WRONG_UCUM:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_CODE_MISMATCH:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_MIN_NO_CODE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_MIN_NO_CONVERT:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_MIN_NO_SYSTEM:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_MIN_NO_VALUE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_NO_QTY:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_NO_UCUM_SVC:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_SYSTEM_MISMATCH:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_VALUE_NO_CODE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_VALUE_NO_SYSTEM:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_VALUE_NO_VALUE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_VALUE_WRONG:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_QTY_MIN_VALUE_WRONG_UCUM:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_STRING_LENGTH:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_STRING_WS:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_STRING_WS_ALL:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_TIME_VALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_URI_OID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_URI_UUID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_URI_WS:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_URL_EXAMPLE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_URL_RESOLVE:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_UUID_STRAT:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_UUID_VALID:
      case I18nConstants.UNICODE_BIDI_CONTROLS_CHARS_MATCH:
      case I18nConstants.UNICODE_XML_BAD_CHARS:
      case I18nConstants.VALIDATION_VAL_PROFILE_MATCHMULTIPLE:
      case I18nConstants.VALIDATION_VAL_PROFILE_NODEFINITION:
      case I18nConstants.VALIDATION_VAL_PROFILE_NOTSLICE:
      case I18nConstants.VALIDATION_VAL_PROFILE_OUTOFORDER:
      case I18nConstants.VALIDATION_VAL_PROFILE_SLICEORDER:
      case I18nConstants.VALIDATION_VAL_PROFILE_WRONGTYPE:
      case I18nConstants.XHTML_URL_INVALID:
      case I18nConstants.XHTML_XHTML_ATTRIBUTE_ILLEGAL:
      case I18nConstants.XHTML_XHTML_DOCTYPE_ILLEGAL:
      case I18nConstants.XHTML_XHTML_ELEMENT_ILLEGAL:
      case I18nConstants.XHTML_XHTML_ELEMENT_ILLEGAL_IN_PARA:
      case I18nConstants.XHTML_XHTML_NAME_INVALID:
      case I18nConstants.XHTML_XHTML_NS_INVALID:
        return OperationOutcome.IssueType.INVALID;
      case I18nConstants.PROBLEM_PROCESSING_EXPRESSION__IN_PROFILE__PATH__:
        return OperationOutcome.IssueType.INVARIANT;
      case I18nConstants.MEASURE_M_LIB_UNKNOWN:
      case I18nConstants.MEASURE_MR_SCORE_UNIT_REQUIRED:
        return OperationOutcome.IssueType.NOTFOUND;
      case I18nConstants.VALIDATION_VAL_PROFILE_NOCHECKMAX:
      case I18nConstants.VALIDATION_VAL_PROFILE_NOCHECKMIN:
      case I18nConstants.VALIDATION_VAL_PROFILE_NOTALLOWED:
        return OperationOutcome.IssueType.NOTSUPPORTED;
      case I18nConstants.SLICING_CANNOT_BE_EVALUATED:
        return OperationOutcome.IssueType.PROCESSING;
      case I18nConstants.BUNDLE_BUNDLE_ENTRY_NOFULLURL:
      case I18nConstants.MEASURE_M_CRITERIA_UNKNOWN:
      case I18nConstants.MEASURE_M_GROUP_CODE:
      case I18nConstants.MEASURE_M_GROUP_POP:
      case I18nConstants.MEASURE_M_GROUP_POP_NO_CODE:
      case I18nConstants.MEASURE_M_GROUP_STRATA_COMP_NO_CODE:
      case I18nConstants.MEASURE_M_GROUP_STRATA_NO_CODE:
      case I18nConstants.MEASURE_M_NO_GROUPS:
      case I18nConstants.MEASURE_MR_M_NONE:
      case I18nConstants.MEASURE_MR_M_NOTFOUND:
      case I18nConstants.MEASURE_MR_SCORE_REQUIRED:
      case I18nConstants.MEASURE_MR_SCORE_VALUE_INVALID_01:
      case I18nConstants.MEASURE_MR_SCORE_VALUE_REQUIRED:
      case I18nConstants.MEASURE_SHAREABLE_EXTRA_MISSING:
      case I18nConstants.MEASURE_SHAREABLE_EXTRA_MISSING_HL7:
      case I18nConstants.MEASURE_SHAREABLE_MISSING:
      case I18nConstants.MEASURE_SHAREABLE_MISSING_HL7:
      case I18nConstants.REFERENCE_REF_NOTFOUND_BUNDLE:
        return OperationOutcome.IssueType.REQUIRED;
      case I18nConstants.DETAILS_FOR__MATCHING_AGAINST_PROFILE_:
      case I18nConstants.DOES_NOT_MATCH_SLICE_:
      case I18nConstants.EXTENSION_EXT_MODIFIER_MISMATCHN:
      case I18nConstants.EXTENSION_EXT_MODIFIER_MISMATCHY:
      case I18nConstants.EXTENSION_EXT_MODIFIER_N:
      case I18nConstants.EXTENSION_EXT_MODIFIER_Y:
      case I18nConstants.EXTENSION_EXT_SIMPLE_ABSENT:
      case I18nConstants.EXTENSION_EXT_SIMPLE_WRONG:
      case I18nConstants.EXTENSION_EXT_TYPE:
      case I18nConstants.EXTENSION_EXT_UNKNOWN:
      case I18nConstants.EXTENSION_EXT_UNKNOWN_NOTHERE:
      case I18nConstants.EXTENSION_EXTM_CONTEXT_WRONG:
      case I18nConstants.EXTENSION_EXTM_CONTEXT_WRONG_XVER:
      case I18nConstants.EXTENSION_EXTP_CONTEXT_WRONG:
      case I18nConstants.EXTENSION_EXTP_CONTEXT_WRONG_XVER:
      case I18nConstants.EXTENSION_PROF_TYPE:
      case I18nConstants.PROFILE__DOES_NOT_MATCH_FOR__BECAUSE_OF_THE_FOLLOWING_PROFILE_ISSUES__:
      case I18nConstants.PROFILE_EXT_NOT_HERE:
      case I18nConstants.REFERENCE_REF_AGGREGATION:
      case I18nConstants.REFERENCE_REF_BADTARGETTYPE:
      case I18nConstants.REFERENCE_REF_BADTARGETTYPE2:
      case I18nConstants.REFERENCE_REF_CANTMATCHCHOICE:
      case I18nConstants.REFERENCE_REF_CANTMATCHTYPE:
      case I18nConstants.REFERENCE_REF_CANTRESOLVE:
      case I18nConstants.REFERENCE_REF_CANTRESOLVEPROFILE:
      case I18nConstants.REFERENCE_REF_MULTIPLEMATCHES:
      case I18nConstants.REFERENCE_REF_NODISPLAY:
      case I18nConstants.REFERENCE_REF_NOTYPE:
      case I18nConstants.REFERENCE_REF_SUSPICIOUS:
      case I18nConstants.REFERENCE_REF_WRONGTARGET:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_ATT_NO_CONTENT:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_ATT_SIZE_CORRECT:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_ATT_SIZE_INVALID:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_ATT_TOO_LONG:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_BASE64_TOO_LONG:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_DECIMAL_CHARS:
      case I18nConstants.VALIDATION_VAL_CONTENT_UNKNOWN:
      case I18nConstants.VALIDATION_VAL_GLOBAL_PROFILE_UNKNOWN:
      case I18nConstants.VALIDATION_VAL_NOTYPE:
      case I18nConstants.VALIDATION_VAL_PROFILE_MAXIMUM:
      case I18nConstants.VALIDATION_VAL_PROFILE_MINIMUM:
      case I18nConstants.VALIDATION_VAL_PROFILE_MULTIPLEMATCHES:
      case I18nConstants.VALIDATION_VAL_PROFILE_NOMATCH:
      case I18nConstants.VALIDATION_VAL_PROFILE_NOSNAPSHOT:
      case I18nConstants.VALIDATION_VAL_PROFILE_NOTYPE:
      case I18nConstants.VALIDATION_VAL_PROFILE_OTHER_VERSION:
      case I18nConstants.VALIDATION_VAL_PROFILE_THIS_VERSION_OK:
      case I18nConstants.VALIDATION_VAL_PROFILE_THIS_VERSION_OTHER:
      case I18nConstants.VALIDATION_VAL_PROFILE_UNKNOWN:
      case I18nConstants.VALIDATION_VAL_PROFILE_UNKNOWN_ERROR:
      case I18nConstants.VALIDATION_VAL_PROFILE_UNKNOWN_ERROR_NETWORK:
      case I18nConstants.VALIDATION_VAL_PROFILE_UNKNOWN_NOT_POLICY:
      case I18nConstants.VALIDATION_VAL_UNKNOWN_PROFILE:
        return OperationOutcome.IssueType.STRUCTURE;
      case I18nConstants.EXTENSION_CONTEXT_UNABLE_TO_CHECK_PROFILE:
      case I18nConstants.EXTENSION_CONTEXT_UNABLE_TO_FIND_PROFILE:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_NOT_USABLE:
      case I18nConstants.TERMINOLOGY_TX_SYSTEM_NOTKNOWN:
        return OperationOutcome.IssueType.UNKNOWN;
      case I18nConstants._DT_FIXED_WRONG:
      case I18nConstants.BUNDLE_MSG_EVENT_COUNT:
      case I18nConstants.EXTENSION_EXT_COUNT_MISMATCH:
      case I18nConstants.EXTENSION_EXT_COUNT_NOTFOUND:
      case I18nConstants.EXTENSION_EXT_FIXED_BANNED:
      case I18nConstants.FIXED_TYPE_CHECKS_DT_ADDRESS_LINE:
      case I18nConstants.FIXED_TYPE_CHECKS_DT_NAME_FAMILY:
      case I18nConstants.FIXED_TYPE_CHECKS_DT_NAME_GIVEN:
      case I18nConstants.FIXED_TYPE_CHECKS_DT_NAME_PREFIX:
      case I18nConstants.FIXED_TYPE_CHECKS_DT_NAME_SUFFIX:
      case I18nConstants.PATTERN_CHECK_STRING:
      case I18nConstants.PROFILE_VAL_MISSINGELEMENT:
      case I18nConstants.PROFILE_VAL_NOTALLOWED:
      case I18nConstants.TERMINOLOGY_TX_CODING_COUNT:
      case I18nConstants.TYPE_CHECKS_FIXED_CC:
      case I18nConstants.TYPE_CHECKS_FIXED_CC_US:
      case I18nConstants.TYPE_CHECKS_PATTERN_CC:
      case I18nConstants.TYPE_CHECKS_PATTERN_CC_US:
      case I18nConstants.TYPE_SPECIFIC_CHECKS_DT_DECIMAL_RANGE:
        return OperationOutcome.IssueType.VALUE;
      default:
        return OperationOutcome.IssueType.NULL;
    }
  }

  /**
   * Initializes a FhirValidator based on the following validation support:
   * <ul>
   *   <li>Default (base R4) profiles</li>
   *   <li>IGs/resources found on the classpath</li>
   *   <li>Specified measure definition bundles</li>
   *   <li>Snapshot-generating support</li>
   *   <li>In-memory terminology service</li>
   *   <li>Common code systems terminology service</li>
   *   <li>Unknown code system warning support</li>
   * </ul>
   */
  private static FhirValidator initialize(List<Bundle> support) {
    FhirContext fhirContext = FhirContextProvider.getFhirContext();
    FhirValidator validator = fhirContext.newValidator();

    MeasureDefinitionBasedValidationSupport measureDefinitionBasedValidationSupport =
            new MeasureDefinitionBasedValidationSupport(fhirContext);
    support.stream()
            .flatMap(bundle -> bundle.getEntry().stream())
            .map(Bundle.BundleEntryComponent::getResource)
            .filter(Objects::nonNull)
            .forEachOrdered(measureDefinitionBasedValidationSupport::addResource);
    ValidationSupportChain validationSupportChain = new ValidationSupportChain(
            new DefaultProfileValidationSupport(fhirContext),
            ClasspathBasedValidationSupport.getInstance(),
            measureDefinitionBasedValidationSupport,
            new SnapshotGeneratingValidationSupport(fhirContext),
            new InMemoryTerminologyServerValidationSupport(fhirContext),
            new CommonCodeSystemsTerminologyService(fhirContext));
    CachingValidationSupport cachingValidationSupport = new CachingValidationSupport(validationSupportChain);
    IValidatorModule validatorModule = new FhirInstanceValidator(cachingValidationSupport);
    validator.registerValidatorModule(validatorModule);

    validator.setExecutorService(ForkJoinPool.commonPool());
    validator.setConcurrentBundleValidation(true);

    return validator;
  }

  public OperationOutcome validateRaw(IBaseResource resource) {
    FhirValidator validator = initialize(List.of());
    OperationOutcome outcome = new OperationOutcome();
    ValidationResult result = validator.validateWithResult(resource);
    result.populateOperationOutcome(outcome);
    return outcome;
  }

  private void validateResource(FhirValidator validator, Resource resource, OperationOutcome outcome, OperationOutcome.IssueSeverity severity) {
    ValidationOptions opts = new ValidationOptions();

    ValidationResult result = validator.validateWithResult(resource, opts);

    for (SingleValidationMessage message : result.getMessages()) {
      OperationOutcome.IssueSeverity messageSeverity = getIssueSeverity(message.getSeverity());

      // Skip the message depending on the severity filter/arg
      if (severity != null) {
        if (severity == OperationOutcome.IssueSeverity.ERROR) {
          if (messageSeverity == OperationOutcome.IssueSeverity.INFORMATION || messageSeverity == OperationOutcome.IssueSeverity.WARNING) {
            continue;
          }
        } else if (severity == OperationOutcome.IssueSeverity.WARNING) {
          if (messageSeverity == OperationOutcome.IssueSeverity.INFORMATION) {
            continue;
          }
        }
      }

      OperationOutcome.IssueType issueCode = getIssueCode(message.getMessageId());

      if (issueCode == OperationOutcome.IssueType.NULL && message.getMessageId() != null) {
        logger.warn("Unknown issue code {} for message {}", message.getMessageId(), message.getMessage());
      }

      outcome.addIssue()
              .setSeverity(messageSeverity)
              .setCode(issueCode)
              .setDetails(new CodeableConcept().setText(message.getMessage()))
              .setExpression(List.of(
                      new StringType(message.getLocationString()),
                      new StringType(message.getLocationLine() + ":" + message.getLocationCol())));
    }
  }

  /**
   * Update the issue locations/expressions to use resource IDs instead of "ofType(MedicationRequest)" (for example).
   * Only works on Bundle resources and looks for patterns such as "Bundle.entry[X].resource.ofType(MedicationRequest)" to replace with "Bundle.entry[X].resource.where(id = '123')"
   *
   * @param resource The resource to amend
   * @param outcome  The outcome to amend
   */
  private void improveIssueExpressions(Resource resource, OperationOutcome outcome) {
    final String regex = "^Bundle.entry\\[(\\d+)\\]\\.resource\\.ofType\\(.+?\\)(.*)$";
    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

    if (resource.getResourceType() == ResourceType.Bundle) {
      Bundle bundle = (Bundle) resource;
      for (OperationOutcome.OperationOutcomeIssueComponent issue : outcome.getIssue()) {
        if (!issue.getExpression().isEmpty()) {
          String location = issue.getExpression().get(0).asStringValue();
          final Matcher matcher = pattern.matcher(location);

          if (matcher.matches()) {
            int entryIndex = Integer.parseInt(matcher.group(1));
            if (entryIndex < bundle.getEntry().size()) {
              String resourceId = bundle.getEntry().get(entryIndex).getResource().getIdElement().getIdPart();
              String resourceType = bundle.getEntry().get(entryIndex).getResource().getResourceType().toString();
              ArrayList newExpressions = new ArrayList<>(issue.getExpression());
              newExpressions.set(0, new StringType("Bundle.entry[" + entryIndex + "].resource.ofType(" + resourceType + ").where(id = '" + resourceId + "')" + matcher.group(2)));
              issue.setExpression(newExpressions);
            }
          }
        }
      }
    }
  }

  /**
   * Validates a resource at the specified severity threshold.
   * Uses the specified measure definition bundles as additional validation support.
   * Optionally skips reinitialization of the underlying FhirValidator.
   * This is only appropriate when validating multiple resources with the same support.
   * E.g., validating individual MeasureReport bundles all generated as part of the same report.
   * In such a case, the Validator instance should not be shared across requests.
   */
  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity, List<Bundle> support, boolean reinitialize) {
    FhirValidator validator;
    if (reinitialize || this.validator == null) {
      validator = initialize(support);
      this.validator = validator;
    } else {
      validator = this.validator;
    }

    logger.debug("Validating {}", resource.getResourceType().toString().toLowerCase());

    OperationOutcome outcome = new OperationOutcome();
    Date start = new Date();

    //noinspection unused
    outcome.setId(UUID.randomUUID().toString());

    this.validateResource(validator, resource, outcome, severity);
    this.improveIssueExpressions(resource, outcome);

    Date end = new Date();
    logger.debug("Validation took {} seconds", TimeUnit.MILLISECONDS.toSeconds(end.getTime() - start.getTime()));
    logger.debug("Validation found {} issues", outcome.getIssue().size());

    // Add extensions (which don't formally exist) that show the total issue count and severity threshold
    outcome.addExtension(Constants.OperationOutcomeTotalExtensionUrl, new IntegerType(outcome.getIssue().size()));
    outcome.addExtension(Constants.OperationOutcomeSeverityExtensionUrl, new CodeType(severity.toCode()));

    return outcome;
  }

  /**
   * Validates a resource at the specified severity threshold.
   * Uses the specified measure definition bundles as additional validation support.
   */
  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity, List<Bundle> support) {
    return validate(resource, severity, support, true);
  }

  /**
   * Validates a resource at the specified severity threshold.
   */
  public OperationOutcome validate(Resource resource, OperationOutcome.IssueSeverity severity) {
    return validate(resource, severity, List.of(), true);
  }
}
