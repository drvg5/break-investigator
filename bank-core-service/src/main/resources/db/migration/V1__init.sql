-- Bank core schema. See docs/decisions.md ADR-001..ADR-005.

create table counterparty (
    id      bigint primary key,
    code    varchar(32)  not null unique,
    name    varchar(200) not null,
    lei     char(20)     not null unique,
    bic     varchar(11)  not null
);

create table instrument (
    id          bigint primary key,
    isin        char(12)     not null unique,
    ticker      varchar(16)  not null,
    name        varchar(200) not null,
    market      varchar(8)   not null,
    currency    char(3)      not null,
    asset_class varchar(16)  not null
);

-- Standing settlement instructions, versioned by effective dates (ADR-004).
create table settlement_instruction (
    id                     bigint primary key,
    counterparty_id        bigint      not null references counterparty (id),
    market                 varchar(8)  not null,
    asset_class            varchar(16) not null,
    custodian_bic          varchar(11) not null,
    account_number         varchar(34) not null,
    place_of_settlement    varchar(11) not null,
    cash_correspondent_bic varchar(11) not null,
    effective_from         date        not null,
    effective_to           date,
    check (effective_to is null or effective_to >= effective_from)
);
create index ix_ssi_cpty_market on settlement_instruction (counterparty_id, market, asset_class);

create table trade (
    id                        bigint primary key,
    trade_ref                 varchar(32)    not null unique,
    counterparty_id           bigint         not null references counterparty (id),
    instrument_id             bigint         not null references instrument (id),
    side                      varchar(4)     not null check (side in ('BUY', 'SELL')),
    quantity                  numeric(20, 4) not null check (quantity > 0),
    price                     numeric(20, 6) not null check (price > 0),
    gross_amount              numeric(24, 2) not null,
    currency                  char(3)        not null,
    trade_date                date           not null,
    settlement_date           date           not null,
    booked_at                 timestamptz    not null,
    last_amended_at           timestamptz,
    -- The SSI this trade was enriched with; null when none could be found.
    settlement_instruction_id bigint references settlement_instruction (id),
    status                    varchar(24)    not null
);

-- Inbound counterparty confirmation. trade_ref is the counterparty quoting our reference,
-- deliberately not a foreign key: it's an external message and may reference garbage.
create table confirmation (
    id                  bigint primary key,
    confirmation_ref    varchar(32)    not null unique,
    trade_ref           varchar(32)    not null,
    counterparty_id     bigint         not null references counterparty (id),
    isin                char(12)       not null,
    counterparty_side   varchar(4)     not null check (counterparty_side in ('BUY', 'SELL')),
    quantity            numeric(20, 4) not null,
    price               numeric(20, 6) not null,
    trade_date          date           not null,
    settlement_date     date           not null,
    custodian_bic       varchar(11)    not null,
    account_number      varchar(34)    not null,
    place_of_settlement varchar(11)    not null,
    remarks             text,
    received_at         timestamptz    not null
);
create index ix_confirmation_trade_ref on confirmation (trade_ref);

-- "break" is a reserved word, hence trade_break. Symptom only, never root cause (ADR-002).
create table trade_break (
    id          bigint primary key,
    break_ref   varchar(32) not null unique,
    trade_id    bigint      not null references trade (id),
    symptom     varchar(32) not null,
    status      varchar(16) not null,
    detected_at timestamptz not null,
    resolved_at timestamptz
);
create index ix_break_status on trade_break (status);

create table break_event (
    id         bigint generated always as identity primary key,
    break_id   bigint      not null references trade_break (id),
    event_type varchar(32) not null,
    actor      varchar(64) not null,
    details    text,
    created_at timestamptz not null
);
create index ix_break_event_break on break_event (break_id);
