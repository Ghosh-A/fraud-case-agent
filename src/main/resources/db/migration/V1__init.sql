create table fraud_case (
    case_id              varchar(40)  primary key,
    masked_customer_ref  varchar(64)  not null,
    home_region          varchar(64),
    status               varchar(24)  not null,
    intake_channel       varchar(24)  not null,
    analyst_id           varchar(64)  not null,
    created_by           varchar(64)  not null,
    reported_at          timestamp    not null,
    created_at           timestamp    not null,
    updated_at           timestamp    not null
) engine = InnoDB;

create table transaction_claim (
    claim_id             varchar(40)   primary key,
    case_id              varchar(40)   not null,
    transaction_ref      varchar(64)   not null,
    amount               decimal(19,2) not null,
    currency             varchar(3)    not null,
    occurred_at          timestamp     not null,
    txn_status           varchar(24)   not null,
    txn_channel          varchar(24)   not null,
    masked_merchant      varchar(96)   not null,
    issue_type           varchar(32)   not null,
    customer_statement   varchar(2000) not null,
    card_possession      varchar(24)   not null,
    merchant_recognised  bit           not null,
    constraint fk_claim_case foreign key (case_id) references fraud_case (case_id)
) engine = InnoDB;

create table evidence_item (
    evidence_id          varchar(48)   primary key,
    case_id              varchar(40)   not null,
    type                 varchar(32)   not null,
    severity             varchar(16)   not null,
    title                varchar(160)  not null,
    value_summary        varchar(1200) not null,
    provenance           varchar(16)   not null,
    source_ref           varchar(64),
    observed_at          timestamp     not null,
    constraint fk_evidence_case foreign key (case_id) references fraud_case (case_id)
) engine = InnoDB;

create table recommendation (
    recommendation_id    varchar(48)   primary key,
    case_id              varchar(40)   not null,
    mode                 varchar(24)   not null,
    classification       varchar(40)   not null,
    route                varchar(32)   not null,
    risk_level           varchar(16)   not null,
    confidence           double        not null,
    rationale            varchar(2000) not null,
    evidence_ids         varchar(1000),
    missing_information  varchar(1500),
    rule_ids             varchar(500),
    tool_trace_ids       varchar(1000),
    human_review_required bit          not null,
    valid                bit           not null,
    created_at           timestamp     not null,
    constraint fk_rec_case foreign key (case_id) references fraud_case (case_id)
) engine = InnoDB;

create table policy_citation (
    citation_id          varchar(48)   primary key,
    recommendation_id    varchar(48)   not null,
    case_id              varchar(40)   not null,
    document_id          varchar(64)   not null,
    doc_version          varchar(16)   not null,
    section              varchar(64),
    chunk_id             varchar(64)   not null,
    score                double        not null,
    passage              varchar(2000) not null,
    constraint fk_citation_case foreign key (case_id) references fraud_case (case_id),
    constraint fk_citation_rec foreign key (recommendation_id) references recommendation (recommendation_id)
) engine = InnoDB;

create table tool_trace (
    trace_id             varchar(48)  primary key,
    case_id              varchar(40)  not null,
    tool_name            varchar(64)  not null,
    request_hash         varchar(64)  not null,
    status               varchar(16)  not null,
    duration_ms          bigint       not null,
    result_summary       varchar(800),
    created_at           timestamp    not null,
    constraint fk_trace_case foreign key (case_id) references fraud_case (case_id)
) engine = InnoDB;

create table review_disposition (
    review_id            varchar(48)   primary key,
    case_id              varchar(40)   not null,
    reviewer_id          varchar(64)   not null,
    disposition          varchar(24)   not null,
    reason               varchar(1500) not null,
    chosen_route         varchar(32),
    recorded_at          timestamp     not null,
    constraint fk_review_case foreign key (case_id) references fraud_case (case_id)
) engine = InnoDB;

create table audit_event (
    event_id             varchar(48)  primary key,
    case_id              varchar(40)  not null,
    event_type           varchar(32)  not null,
    actor_type           varchar(16)  not null,
    actor_id             varchar(64),
    payload_hash         varchar(64)  not null,
    summary              varchar(500),
    recorded_at          timestamp    not null
) engine = InnoDB;

create table chat_memory (
    id                   bigint       auto_increment primary key,
    case_id              varchar(40)  not null,
    seq                  int          not null,
    role                 varchar(16)  not null,
    content              varchar(2000) not null,
    created_at           timestamp    not null
) engine = InnoDB;

create index idx_claim_case on transaction_claim (case_id);
create index idx_evidence_case on evidence_item (case_id);
create index idx_rec_case on recommendation (case_id);
create index idx_citation_case on policy_citation (case_id);
create index idx_citation_rec on policy_citation (recommendation_id);
create index idx_trace_case on tool_trace (case_id);
create index idx_review_case on review_disposition (case_id);
create index idx_audit_case on audit_event (case_id);
create index idx_memory_case on chat_memory (case_id, seq);
