-- Canonical Supabase SQL Migration
-- DocTranslate AI (v2)

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-----------------------------------------
-- 1. PROFILES TABLE
-----------------------------------------
CREATE TABLE public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    name TEXT,
    avatar_url TEXT,
    default_source_language TEXT DEFAULT 'auto',
    default_target_language TEXT DEFAULT 'en',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable RLS on profiles
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Profiles RLS Policies
CREATE POLICY "Allow users to read their own profile" 
ON public.profiles 
FOR SELECT 
USING (auth.uid() = id);

CREATE POLICY "Allow users to update their own profile" 
ON public.profiles 
FOR UPDATE 
USING (auth.uid() = id);

-----------------------------------------
-- 2. AUTOMATIC PROFILE CREATION TRIGGER
-----------------------------------------
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.profiles (id, email, name, avatar_url, default_source_language, default_target_language)
    VALUES (
        new.id,
        new.email,
        COALESCE(new.raw_user_meta_data->>'name', split_part(new.email, '@', 1)),
        new.raw_user_meta_data->>'avatar_url',
        'auto',
        'en'
    );
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE OR REPLACE TRIGGER on_auth_user_created
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-----------------------------------------
-- 3. DOCUMENTS TABLE (Future-proofing)
-----------------------------------------
CREATE TABLE public.documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    title TEXT NOT NULL,
    file_type TEXT NOT NULL, -- 'image' or 'pdf'
    source_language TEXT NOT NULL DEFAULT 'auto',
    target_language TEXT NOT NULL DEFAULT 'en',
    ocr_text TEXT,
    translated_text TEXT,
    translations JSONB DEFAULT '{}'::jsonb NOT NULL,
    summary JSONB, -- Strict JSON object with 5 keys
    storage_path TEXT,
    thumbnail_path TEXT,
    status TEXT NOT NULL DEFAULT 'processing', -- 'processing' | 'ready' | 'failed'
    error_message TEXT,
    page_count INTEGER DEFAULT 1,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    storage_size_bytes BIGINT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable RLS on documents
ALTER TABLE public.documents ENABLE ROW LEVEL SECURITY;

-- Documents RLS Policies
CREATE POLICY "Allow users to read their own documents" 
ON public.documents 
FOR SELECT 
USING (auth.uid() = user_id);

CREATE POLICY "Allow users to insert their own documents" 
ON public.documents 
FOR INSERT 
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Allow users to update their own documents" 
ON public.documents 
FOR UPDATE 
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Allow users to delete their own documents" 
ON public.documents 
FOR DELETE 
USING (auth.uid() = user_id);

-----------------------------------------
-- 4. DOCUMENT CHATS TABLE (Future-proofing)
-----------------------------------------
CREATE TABLE public.document_chats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID REFERENCES public.documents(id) ON DELETE CASCADE NOT NULL,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE NOT NULL,
    role TEXT NOT NULL, -- 'user' | 'assistant' | 'system'
    message TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable RLS on document_chats
ALTER TABLE public.document_chats ENABLE ROW LEVEL SECURITY;

-- Document Chats RLS Policies
CREATE POLICY "Allow users to read their own document chats" 
ON public.document_chats 
FOR SELECT 
USING (auth.uid() = user_id);

CREATE POLICY "Allow users to insert their own document chats" 
ON public.document_chats 
FOR INSERT 
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Allow users to delete their own document chats" 
ON public.document_chats 
FOR DELETE 
USING (auth.uid() = user_id);

-----------------------------------------
-- 5. STORAGE BUCKETS (Future-proofing)
-----------------------------------------
-- These policies configure permissions on public storage buckets.
-- Note: Requires storage extension to be active.
INSERT INTO storage.buckets (id, name, public) 
VALUES ('doctranslate-documents', 'doctranslate-documents', false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO storage.buckets (id, name, public) 
VALUES ('doctranslate-tts', 'doctranslate-tts', false)
ON CONFLICT (id) DO NOTHING;

-- Storage RLS Policies (for objects under 'doctranslate-documents')
CREATE POLICY "Allow individual read access to their own document storage"
ON storage.objects FOR SELECT
USING (bucket_id = 'doctranslate-documents' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE POLICY "Allow individual insert access to their own document storage"
ON storage.objects FOR INSERT
WITH CHECK (bucket_id = 'doctranslate-documents' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE POLICY "Allow individual update access to their own document storage"
ON storage.objects FOR UPDATE
USING (bucket_id = 'doctranslate-documents' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE POLICY "Allow individual delete access to their own document storage"
ON storage.objects FOR DELETE
USING (bucket_id = 'doctranslate-documents' AND (storage.foldername(name))[1] = auth.uid()::text);
