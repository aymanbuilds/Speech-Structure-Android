from flask import Flask, request, jsonify, url_for
from flask_sqlalchemy import SQLAlchemy
from flask_bcrypt import Bcrypt
from flask_cors import CORS  
from datetime import datetime
from sqlalchemy import func
from transformers import DistilBertTokenizer, DistilBertForSequenceClassification
from transformers import pipeline
import torch
import spacy
from collections import defaultdict
import json
from sentence_transformers import SentenceTransformer, util
from transformers import DistilBertTokenizer, DistilBertModel
from torch.nn.functional import cosine_similarity

nlp = spacy.load('en_core_web_lg')
model = SentenceTransformer('all-MiniLM-L6-v2')

def extract_keywords(text):
    doc = nlp(text.lower())
    keywords = set()
    for token in doc:
        if token.pos_ in ['NOUN', 'ADJ', 'PROPN']:
            keywords.add(token.lemma_)
    return keywords

def create_mapping(questions):
    mapping = defaultdict(set)
    for question in questions:
        keywords = extract_keywords(question)
        for keyword in keywords:
            mapping[keyword].add(question)
    return mapping

#tokenizer = DistilBertTokenizer.from_pretrained('distilbert-base-uncased')
#model = DistilBertForSequenceClassification.from_pretrained('distilbert-base-uncased')

tokenizer = DistilBertTokenizer.from_pretrained('distilbert-base-uncased')
model = DistilBertModel.from_pretrained('distilbert-base-uncased')

classifier = pipeline('text-classification', model=model, tokenizer=tokenizer)

app = Flask(__name__)

CORS(app)

app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///site.db'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False

db = SQLAlchemy(app)
bcrypt = Bcrypt(app)

class User(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(100), unique=True, nullable=False)
    password = db.Column(db.String(100), nullable=False)
    email = db.Column(db.String(100), nullable=False)
    date_created = db.Column(db.DateTime, default=datetime.utcnow)
    status = db.Column(db.String(10), nullable=False, default='active')
    role = db.Column(db.String(20), nullable=False, default='user') 

    def __repr__(self):
        return f"User('{self.username}', '{self.date_created}', '{self.status}', '{self.role}')"

class Terms(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    welcome_message = db.Column(db.Text, nullable=False)
    logo_image = db.Column(db.String(255), nullable=True)
    terms_text = db.Column(db.Text, nullable=False)
    
class ReadTemplate(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    category = db.Column(db.String(50))
    question_number = db.Column(db.String(10))
    question_media = db.Column(db.String(100))
    question = db.Column(db.String(255))
    response_type = db.Column(db.String(50))
    answer_number = db.Column(db.String(10))
    answer_including_media = db.Column(db.String(100))
    next_question = db.Column(db.String(10))

    def __repr__(self):
        return f"ReadTemplate('{self.category}', '{self.question}', '{self.response_type}')"
    
class Answer(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(150), nullable=False)
    start_time = db.Column(db.DateTime, nullable=False, default=datetime.utcnow)
    end_time = db.Column(db.DateTime, nullable=False)
    date = db.Column(db.DateTime, nullable=False, default=datetime.utcnow)
    question = db.Column(db.Text, nullable=False)
    answer = db.Column(db.Text, nullable=False)
    
def setup_mapping():
    questions = ReadTemplate.query.with_entities(ReadTemplate.question).all()
    questions = [q[0] for q in questions]
    global QUESTION_TO_ANSWER_MAPPING
    QUESTION_TO_ANSWER_MAPPING = create_mapping(questions)

def add_admin():
    with app.app_context():  
        admin_username = 'admin'
        admin_password = '123456'
        admin_email = 'admin@gmail.com'
        hashed_password = bcrypt.generate_password_hash(admin_password).decode('utf-8')
        admin_user = User(username=admin_username, password=hashed_password, email=admin_email, status='active', role='admin')
        
        if not User.query.filter_by(username=admin_username).first():
            db.session.add(admin_user)
            db.session.commit()
            print("Admin user added.")

def add_regular_user():
    with app.app_context():
        user_username = 'user'
        user_password = '123456'
        user_email = 'user@gmail.com'
        hashed_password = bcrypt.generate_password_hash(user_password).decode('utf-8')
        regular_user = User(username=user_username, password=hashed_password,email=user_email, status='active', role='user')
        
        if not User.query.filter_by(username=user_username).first():
            db.session.add(regular_user)
            db.session.commit()
            print("Regular user added.")

def add_terms_entry():
    with app.app_context():
        terms = Terms(
            welcome_message='Welcome to our service!',
            logo_image='static/icons/logo.png', 
            terms_text='Please read our terms and conditions'
        )
        
        if not Terms.query.first():
            db.session.add(terms)
            db.session.commit()
            print("Terms entry added.")

def insert_read_template_data():
    with app.app_context():
        data = [
            {'category': 'C0', 'question_number': 'Feature 1', 'question_media': '', 'question': 'What is your first name?', 'response_type': 'Open response', 'answer_number': 'A', 'answer_including_media': '', 'next_question': '2'},
            {'category': 'C0', 'question_number': 'Feature 2', 'question_media': '', 'question': 'What is your last name?', 'response_type': 'Open response', 'answer_number': 'A', 'answer_including_media': '', 'next_question': '3'},
            {'category': 'C0', 'question_number': 'Feature 3', 'question_media': '', 'question': 'Please provide your date of birth in dd/mm/yyyy format.', 'response_type': 'Open response', 'answer_number': 'A', 'answer_including_media': '', 'next_question': '4'},
            {'category': 'C0', 'question_number': 'Feature 4', 'question_media': '', 'question': 'What is your gender?', 'response_type': 'Open response', 'answer_number': 'A', 'answer_including_media': '', 'next_question': '5'},
            {'category': 'C0', 'question_number': 'Feature 5', 'question_media': '', 'question': 'Kindly provide your phone number', 'response_type': 'Open response', 'answer_number': 'A', 'answer_including_media': '', 'next_question': '6'}
        ]

        for item in data:
            record = ReadTemplate(**item)
            db.session.add(record)
        db.session.commit()

add_admin()
add_regular_user()
add_terms_entry()
#insert_read_template_data()

@app.route('/users', methods=['GET'])
def get_users():
    users = User.query.all() 
    users_list = [
        {
            'id': user.id,
            'username': user.username,
            'email': user.email,
            'date_created': user.date_created.strftime('%Y-%m-%d %H:%M:%S'),
            'status': user.status,
            'role': user.role
        }
        for user in users
    ]
    return jsonify(users_list)

@app.route('/users/add', methods=['POST'])
def create_user():
    try:
        data = request.get_json()
        username = data['username']
        password = data['password']
        email = data['email']
        role = data.get('role', 'user')

        if User.query.filter_by(username=username).first():
            return jsonify({'error': 'Username already exists'}), 400

        new_user = User(username=username, password=password,email=email, role=role)
        db.session.add(new_user)
        db.session.commit()

        return jsonify({
            'id': new_user.id,
            'username': new_user.username,
            'email': new_user.email,
            'date_created': new_user.date_created.strftime('%Y-%m-%d %H:%M:%S'),
            'status': new_user.status,
            'role': new_user.role
        }), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500

@app.route('/api/terms', methods=['GET'])
def get_first_terms():
    terms = Terms.query.first()
    if terms:
        logo_image_url = url_for('static', filename=terms.logo_image, _external=True)
        return jsonify({
            'welcome_message': terms.welcome_message,
            'logo_image': logo_image_url,
            'terms_text': terms.terms_text
        })
    return jsonify({'error': 'No terms found'}), 404

@app.route('/api/terms/write', methods=['POST'])
def update_terms():
    data = request.json
    welcome_message = data.get('welcome_message')
    logo_image = data.get('logo_image')
    terms_text = data.get('terms_text')

    terms = Terms.query.first()
    if terms:
        terms.welcome_message = welcome_message
        terms.logo_image = logo_image
        terms.terms_text = terms_text
    else:
        terms = Terms(welcome_message=welcome_message, logo_image=logo_image, terms_text=terms_text)
        db.session.add(terms)

    db.session.commit()

    return jsonify({'message': 'Terms updated successfully'})

@app.route('/api/login', methods=['POST'])
def login():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')
    
    print(f"Attempted login with username: {username} and password: {password}")
    
    user = User.query.filter(func.lower(User.username) == func.lower(username)).first()
    if user and bcrypt.check_password_hash(user.password, password):
        return jsonify({
            'id': user.id,
            'username': user.username,
            'date_created': user.date_created.strftime('%Y-%m-%d %H:%M:%S'),
            'status': user.status,
            'role': user.role
        })
    
    return jsonify({'error': 'Invalid username or password'}), 401

@app.route('/api/read_template', methods=['GET'])
def get_read_template():
    try:
        templates = ReadTemplate.query.all()

        template_list = [
            {
                'id': template.id,
                'category': template.category,
                'question_number': template.question_number,
                'question_media': template.question_media,
                'question': template.question,
                'response_type': template.response_type,
                'answer_number': template.answer_number,
                'answer_including_media': template.answer_including_media,
                'next_question': template.next_question
            }
            for template in templates
        ]

        return jsonify(template_list)

    except Exception as e:
        return jsonify({'error': str(e)}), 500
    
@app.route('/api/template/write', methods=['POST']) 
def add_template():
    try:
        data = request.json

        category = data.get('category')
        question_number = data.get('question_number')
        question_media = data.get('question_media')
        question = data.get('question')
        response_type = data.get('response_type')
        answer_number = data.get('answer_number')
        answer_including_media = data.get('answer_including_media')
        next_question = data.get('next_question')

        new_template = ReadTemplate(
            category=category,
            question_number=question_number,
            question_media=question_media,
            question=question,
            response_type=response_type,
            answer_number=answer_number,
            answer_including_media=answer_including_media,
            next_question=next_question
        )

        db.session.add(new_template)
        db.session.commit()

        return jsonify({'message': 'Template added successfully'}), 200

    except Exception as e: 
        print(e)
        return jsonify({'error': str(e)}), 500
    
@app.route('/add_answer', methods=['GET'])
def add_answer():
    username = request.args.get('username')
    start_time = request.args.get('start_time')
    end_time = request.args.get('end_time')
    date = request.args.get('date')
    question = request.args.get('question')
    answer = request.args.get('answer')

    start_time = datetime.strptime(start_time, '%Y-%m-%d %H:%M:%S')
    end_time = datetime.strptime(end_time, '%Y-%m-%d %H:%M:%S')
    date = datetime.strptime(date, '%Y-%m-%d %H:%M:%S')

    new_answer = Answer(
        username=username,
        start_time=start_time,
        end_time=end_time,
        date=date,
        question=question,
        answer=answer
    )

    db.session.add(new_answer)
    db.session.commit()

    return "Answer added successfully!"

def classify_text(text):
    doc = nlp(text)
    return doc.cats

@app.route('/correct_answers', methods=['POST'])
def correct_answers():
    username = request.json.get('username')
    if not username:
        return jsonify({"error": "Username is required"}), 400

    # Get all answers for the given username
    answers = Answer.query.filter_by(username=username).all()

    # Create a list to store the updated answers
    updated_answers = []

    # Loop through each answer
    for answer in answers:
        question = answer.question
        answer_text = answer.answer

        # Initialize variables to track the best match
        best_match = None
        highest_score = -1

        # Check the match score for the current answer against all questions
        for template in ReadTemplate.query.all():
            template_question = template.question
            
            # Get match score for the answer and template question
            score = get_match_score(answer_text, template_question)
            
            # Update the best match if the current score is higher
            if score > highest_score:
                highest_score = score
                best_match = answer_text

        if best_match and highest_score > 0:
            # Update the answer with the best match
            answer.answer = best_match
            db.session.commit()
            
            updated_answers.append({
                "updated_answer": best_match,
                "question": question
            })

    return jsonify(updated_answers)

def is_appropriate_answer(answer, question):
    """
    Determine if an answer is appropriate for a question using semantic similarity.
    """
    # Process both answer and question with spaCy
    answer_doc = nlp(answer)
    question_doc = nlp(question)
    
    # Calculate semantic similarity
    similarity = answer_doc.similarity(question_doc)
    
    threshold = 0.5  
    
    return similarity > threshold

def preprocess_text(text):
    """
    Preprocess text by lowercasing and stripping extra spaces.
    """
    return text.lower().strip()

def get_embeddings(text):
    """
    Get embeddings for the given text using DistilBERT.
    """
    inputs = tokenizer(text, return_tensors='pt', truncation=True, padding=True)
    outputs = model(**inputs)
    # Take the mean of the token embeddings to get a sentence-level embedding
    return outputs.last_hidden_state.mean(dim=1)

def get_match_score(answer, question):
    """
    Score how well an answer matches a question using semantic similarity with Sentence-BERT.
    """
    # Get embeddings for both answer and question
    answer_embedding = get_embeddings(answer)
    question_embedding = get_embeddings(question)
    
    # Calculate cosine similarity
    #similarity = util.pytorch_cos_sim(answer_embedding, question_embedding).item()
    similarity = cosine_similarity(answer_embedding, question_embedding).item()
    
    return similarity

with app.app_context():
    db.create_all()
    #setup_mapping()
    
    # question = "Kindly provide your phone number"

    # answers = [
    #     "0611445588",  # Expected correct answer
    #     "male",         # Unrelated answer
    #     "123-456-7890", # Another phone number
    # ]

    # # Initialize variables to keep track of the highest score and corresponding answer
    # highest_score = -1
    # best_answer = ""

    # for answer in answers:
    #     score = get_match_score(answer, question)
    #     print(f"Testing with answer: '{answer}'")
    #     print(f"Match Score: {score}")
        
    #     # Update the highest score and best answer if the current score is higher
    #     if score > highest_score:
    #         highest_score = score
    #         best_answer = answer

    # # Print the answer with the highest score
    # print(f"\nThe answer with the highest score is: '{best_answer}'")
    # print(f"Highest Score: {highest_score}")

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)